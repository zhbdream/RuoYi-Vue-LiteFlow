package com.ruoyiliteflow.liteflow.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowComponentVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowRouteResultVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowStreamEventVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;
import com.ruoyiliteflow.liteflow.service.ILfExecLogService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowWebhookService;
import com.ruoyiliteflow.liteflow.service.impl.LfScriptServiceImpl;
import com.ruoyiliteflow.liteflow.support.AgentStreamMode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeForComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.LiteflowResponse;

@Service
public class LiteFlowExecuteServiceImpl implements ILiteFlowExecuteService
{
    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ILfChainService lfChainService;

    @Autowired
    private LfChainMapper lfChainMapper;

    @Autowired
    private ILfExecLogService lfExecLogService;

    @Autowired
    private ILfChainPermissionService lfChainPermissionService;

    @Autowired
    private ILiteFlowWebhookService liteFlowWebhookService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public LiteFlowExecuteResultVo execute(String chainName, Object param)
    {
        return execute(chainName, param, null);
    }

    @Override
    public LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy)
    {
        return execute(chainName, param, createBy, false);
    }

    @Override
    public LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy, boolean bypassChainPermission)
    {
        long start = System.currentTimeMillis();
        lfChainPermissionService.assertCanExecute(chainName, bypassChainPermission);
        LiteFlowExecuteResultVo result = doExecute(chainName, param, null);
        long duration = System.currentTimeMillis() - start;
        afterExecute(chainName, param, result, duration, createBy);
        return result;
    }

    @Override
    public LiteFlowExecuteResultVo executeStream(String chainName, Object param, String createBy,
            Consumer<LiteFlowStreamEventVo> listener)
    {
        long start = System.currentTimeMillis();
        lfChainPermissionService.assertCanExecute(chainName, false);
        AgentStreamMode.enable();
        try
        {
            LiteFlowExecuteResultVo result = doExecute(chainName, param, listener);
            long duration = System.currentTimeMillis() - start;
            afterExecute(chainName, param, result, duration, createBy);
            return result;
        }
        finally
        {
            AgentStreamMode.clear();
        }
    }

    private void afterExecute(String chainName, Object param, LiteFlowExecuteResultVo result, long duration, String createBy)
    {
        try
        {
            lfExecLogService.saveExecuteLog(chainName, param, result, duration, createBy);
        }
        catch (Exception ignored)
        {
            // 日志写入失败不影响执行结果
        }
        try
        {
            liteFlowWebhookService.notifyAsync(chainName, param, result, duration, createBy);
        }
        catch (Exception ignored)
        {
            // Webhook 失败不影响执行结果
        }
    }

    @Override
    public LiteFlowExecuteResultVo executeWithEl(String elData, Object param, String contextClass, String createBy)
    {
        if (StringUtils.isEmpty(elData))
        {
            throw new ServiceException("EL 表达式不能为空");
        }
        long start = System.currentTimeMillis();
        LiteflowResponse response = invokeWithContext(elData.trim(), param, contextClass, true);
        LiteFlowExecuteResultVo result = toResultVo(response, "(EL调试)");
        try
        {
            lfExecLogService.saveExecuteLog("(EL调试)", param, result, System.currentTimeMillis() - start, createBy);
        }
        catch (Exception ignored)
        {
            // ignore
        }
        return result;
    }

    @Override
    public LiteFlowRouteResultVo executeRouteChain(String namespace, Object param, String contextClass, String createBy)
    {
        long start = System.currentTimeMillis();
        List<LiteflowResponse> responses;
        if (StringUtils.isNotEmpty(namespace))
        {
            responses = invokeRouteWithContext(namespace, param, contextClass);
        }
        else
        {
            responses = invokeRouteWithContext(null, param, contextClass);
        }
        LiteFlowRouteResultVo routeResult = new LiteFlowRouteResultVo();
        routeResult.setNamespace(namespace);
        List<LiteFlowExecuteResultVo> results = new ArrayList<>();
        if (responses != null)
        {
            for (LiteflowResponse response : responses)
            {
                String chainId = response.getChainId();
                LiteFlowExecuteResultVo item = toResultVo(response, chainId);
                if (StringUtils.isNotEmpty(contextClass))
                {
                    fillContextData(item, response, contextClass);
                }
                results.add(item);
                try
                {
                    lfExecLogService.saveExecuteLog(
                        StringUtils.isNotEmpty(chainId) ? chainId : "(route)",
                        param,
                        item,
                        System.currentTimeMillis() - start,
                        createBy);
                }
                catch (Exception ignored)
                {
                    // ignore
                }
            }
        }
        routeResult.setResults(results);
        routeResult.setHitCount(results.size());
        return routeResult;
    }

    private LiteflowResponse invokeWithContext(String elOrChain, Object param, String contextClass, boolean withEl)
    {
        if (StringUtils.isNotEmpty(contextClass))
        {
            try
            {
                Class<?> clazz = Class.forName(contextClass);
                if (withEl)
                {
                    return flowExecutor.execute2RespWithEL(elOrChain, param, null, clazz);
                }
                return flowExecutor.execute2Resp(elOrChain, param, clazz);
            }
            catch (ClassNotFoundException e)
            {
                throw new ServiceException("上下文类不存在: " + contextClass);
            }
        }
        if (withEl)
        {
            return flowExecutor.execute2RespWithEL(elOrChain, param);
        }
        return flowExecutor.execute2Resp(elOrChain, param);
    }

    private List<LiteflowResponse> invokeRouteWithContext(String namespace, Object param, String contextClass)
    {
        if (StringUtils.isNotEmpty(contextClass))
        {
            try
            {
                Class<?> clazz = Class.forName(contextClass);
                if (StringUtils.isNotEmpty(namespace))
                {
                    return flowExecutor.executeRouteChain(namespace, param, clazz);
                }
                return flowExecutor.executeRouteChain(param, clazz);
            }
            catch (ClassNotFoundException e)
            {
                throw new ServiceException("上下文类不存在: " + contextClass);
            }
        }
        if (StringUtils.isNotEmpty(namespace))
        {
            return flowExecutor.executeRouteChain(namespace, param);
        }
        return flowExecutor.executeRouteChain(param);
    }

    private LiteFlowExecuteResultVo toResultVo(LiteflowResponse response, String chainId)
    {
        LiteFlowExecuteResultVo result = new LiteFlowExecuteResultVo();
        result.setChainId(StringUtils.isNotEmpty(response.getChainId()) ? response.getChainId() : chainId);
        result.setSuccess(response.isSuccess());
        result.setCode(response.getCode());
        result.setMessage(response.getMessage());
        result.setRequestId(response.getRequestId());
        result.setExecuteStepStr(response.getExecuteStepStr());
        result.setExecuteStepStrWithTime(response.getExecuteStepStrWithTime());
        if (!response.isSuccess())
        {
            result.setFailedNodeId(resolveFailedNodeId(response.getExecuteStepStr(), response.getMessage()));
        }
        return result;
    }

    private void fillContextData(LiteFlowExecuteResultVo result, LiteflowResponse response, String contextClass)
    {
        try
        {
            Class<?> clazz = Class.forName(contextClass);
            Object contextBean = response.getContextBean(clazz);
            if (contextBean != null)
            {
                result.setContextData(JSON.parseObject(JSON.toJSONString(contextBean)));
            }
        }
        catch (ClassNotFoundException ignored)
        {
            // ignore
        }
    }

    private LiteFlowExecuteResultVo doExecute(String chainName, Object param)
    {
        return doExecute(chainName, param, null);
    }

    private LiteFlowExecuteResultVo doExecute(String chainName, Object param, Consumer<LiteFlowStreamEventVo> listener)
    {
        LfChain chain = lfChainService.selectLfChainByName(chainName);
        if (chain == null)
        {
            throw new ServiceException("链路不存在: " + chainName);
        }
        if (!"0".equals(chain.getDraftFlag()) || !"0".equals(chain.getStatus()) || chain.getEnable() == null || chain.getEnable() != 1)
        {
            throw new ServiceException("链路未启用或处于草稿/停用状态: " + chainName);
        }

        ExecuteOption option = buildExecuteOption(chain, listener);

        LiteflowResponse response;
        if (option != null)
        {
            response = flowExecutor.execute2Resp(chainName, param, option);
        }
        else if (StringUtils.isNotEmpty(chain.getContextClass()))
        {
            try
            {
                Class<?> contextClass = Class.forName(chain.getContextClass());
                response = flowExecutor.execute2Resp(chainName, param, contextClass);
            }
            catch (ClassNotFoundException e)
            {
                throw new ServiceException("上下文类不存在: " + chain.getContextClass());
            }
        }
        else
        {
            response = flowExecutor.execute2Resp(chainName, param);
        }

        LiteFlowExecuteResultVo result = new LiteFlowExecuteResultVo();
        result.setChainId(chainName);
        result.setSuccess(response.isSuccess());
        result.setCode(response.getCode());
        result.setMessage(response.getMessage());
        result.setRequestId(response.getRequestId());
        result.setExecuteStepStr(response.getExecuteStepStr());
        result.setExecuteStepStrWithTime(response.getExecuteStepStrWithTime());
        if (!response.isSuccess())
        {
            result.setFailedNodeId(resolveFailedNodeId(response.getExecuteStepStr(), response.getMessage()));
        }

        if (StringUtils.isNotEmpty(chain.getContextClass()))
        {
            try
            {
                Class<?> contextClass = Class.forName(chain.getContextClass());
                Object contextBean = response.getContextBean(contextClass);
                if (contextBean != null)
                {
                    result.setContextData(JSON.parseObject(JSON.toJSONString(contextBean)));
                }
            }
            catch (ClassNotFoundException ignored)
            {
                // ignore
            }
        }
        return result;
    }

    private ExecuteOption buildExecuteOption(LfChain chain, Consumer<LiteFlowStreamEventVo> listener)
    {
        if (listener == null && StringUtils.isEmpty(chain.getContextClass()))
        {
            return null;
        }
        ExecuteOption option = ExecuteOption.of();
        if (StringUtils.isNotEmpty(chain.getContextClass()))
        {
            try
            {
                Class<?> contextClass = Class.forName(chain.getContextClass());
                option.contextClass(contextClass);
            }
            catch (ClassNotFoundException e)
            {
                throw new ServiceException("上下文类不存在: " + chain.getContextClass());
            }
        }
        if (listener != null)
        {
            option.eventListener(event -> safePublish(listener, event));
        }
        return option;
    }

    private void safePublish(Consumer<LiteFlowStreamEventVo> listener, FlowEvent event)
    {
        if (listener == null || event == null)
        {
            return;
        }
        try
        {
            LiteFlowStreamEventVo vo = new LiteFlowStreamEventVo();
            vo.setType(event.getType());
            vo.setChainId(event.getChainId());
            vo.setNodeId(event.getNodeId());
            vo.setRequestId(event.getRequestId());
            vo.setConversationId(event.getConversationId());
            vo.setText(event.getText());
            vo.setLast(event.isLast());
            vo.setTimestamp(event.getTimestamp());
            if (event.getData() != null)
            {
                Object data = event.getData();
                if (data instanceof Map)
                {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) data;
                    vo.setData(map);
                }
                else
                {
                    Map<String, Object> wrap = new HashMap<>(2);
                    wrap.put("value", data);
                    vo.setData(wrap);
                }
            }
            listener.accept(vo);
        }
        catch (Exception ignored)
        {
            // listener 异常不得打断链路
        }
    }

    private String resolveFailedNodeId(String executeStepStr, String message)
    {
        if (StringUtils.isNotEmpty(executeStepStr))
        {
            String[] parts = executeStepStr.split("==>");
            if (parts.length > 0)
            {
                String last = parts[parts.length - 1].trim();
                if (StringUtils.isNotEmpty(last))
                {
                    return last;
                }
            }
        }
        if (StringUtils.isNotEmpty(message))
        {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("component\\[([a-zA-Z_]\\w*)\\]")
                    .matcher(message);
            if (matcher.find())
            {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public List<LiteFlowComponentVo> listComponents()
    {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(LiteflowComponent.class);
        List<LiteFlowComponentVo> list = new ArrayList<>();
        for (Object bean : beans.values())
        {
            LiteflowComponent annotation = bean.getClass().getAnnotation(LiteflowComponent.class);
            if (annotation == null)
            {
                continue;
            }
            LiteFlowComponentVo vo = new LiteFlowComponentVo();
            vo.setNodeId(resolveNodeId(annotation));
            vo.setName(resolveNodeName(annotation, bean.getClass()));
            vo.setNodeType(resolveNodeType(bean));
            vo.setClassName(bean.getClass().getName());
            list.add(vo);
        }
        list.sort(Comparator.comparing(LiteFlowComponentVo::getNodeId));
        return list;
    }

    @Override
    public List<LiteFlowComponentVo> listComponentsWithRefs()
    {
        List<LiteFlowComponentVo> list = listComponents();
        for (LiteFlowComponentVo vo : list)
        {
            List<String> refs = findChainsReferencingNode(vo.getNodeId());
            vo.setRefChains(refs);
            vo.setRefCount(refs.size());
        }
        return list;
    }

    @Override
    public List<String> findChainsReferencingNode(String nodeId)
    {
        return LfScriptServiceImpl.findChainsReferencingNode(lfChainMapper, nodeId);
    }

    @Override
    public boolean chainContainsAgent(String chainName)
    {
        if (StringUtils.isEmpty(chainName))
        {
            return false;
        }
        LfChain chain = lfChainService.selectLfChainByName(chainName);
        if (chain == null || StringUtils.isEmpty(chain.getElData()))
        {
            return false;
        }
        String el = chain.getElData();
        Set<String> agentIds = new HashSet<>();
        for (LiteFlowComponentVo comp : listComponents())
        {
            if ("agent".equals(comp.getNodeType()) && StringUtils.isNotEmpty(comp.getNodeId()))
            {
                agentIds.add(comp.getNodeId());
            }
        }
        if (agentIds.isEmpty())
        {
            return false;
        }
        for (String agentId : agentIds)
        {
            if (el.contains(agentId))
            {
                return true;
            }
        }
        return false;
    }

    private String resolveNodeId(LiteflowComponent annotation)
    {
        if (StringUtils.isNotEmpty(annotation.id()))
        {
            return annotation.id();
        }
        if (StringUtils.isNotEmpty(annotation.value()))
        {
            return annotation.value();
        }
        return "";
    }

    private String resolveNodeName(LiteflowComponent annotation, Class<?> clazz)
    {
        if (StringUtils.isNotEmpty(annotation.name()))
        {
            return annotation.name();
        }
        String nodeId = resolveNodeId(annotation);
        if (StringUtils.isNotEmpty(nodeId))
        {
            return nodeId;
        }
        return clazz.getSimpleName();
    }

    private String resolveNodeType(Object bean)
    {
        if (isReActAgentComponent(bean))
        {
            return "agent";
        }
        if (bean instanceof NodeBooleanComponent)
        {
            return "boolean";
        }
        if (bean instanceof NodeSwitchComponent)
        {
            return "switch";
        }
        if (bean instanceof NodeForComponent)
        {
            return "for";
        }
        if (bean instanceof NodeIteratorComponent)
        {
            return "iterator";
        }
        if (bean instanceof NodeComponent)
        {
            return "common";
        }
        // 声明式组件（POJO + @LiteflowMethod）
        return "declarative";
    }

    /** 通过类名识别，避免 core 模块强依赖 agent 模块 */
    private boolean isReActAgentComponent(Object bean)
    {
        Class<?> c = bean.getClass();
        while (c != null)
        {
            if ("com.yomahub.liteflow.agent.component.ReActAgentComponent".equals(c.getName()))
            {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
