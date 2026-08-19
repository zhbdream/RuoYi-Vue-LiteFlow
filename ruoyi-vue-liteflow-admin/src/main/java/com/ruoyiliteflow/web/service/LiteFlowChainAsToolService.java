package com.ruoyiliteflow.web.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.aikit.platform.service.IAiToolService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainCase;
import com.ruoyiliteflow.liteflow.mapper.LfChainCaseMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

/**
 * 已发布链路登记为 {@code liteflow-chain} 工具，供智能体本地调用；可选同步开放 MCP。
 */
@Service
public class LiteFlowChainAsToolService
{
    public static final String TOOL_TYPE = "liteflow-chain";
    public static final String TOOL_CODE_PREFIX = "lf_";
    public static final String MCP_SERVER = "liteflow";

    private final ILfChainService lfChainService;
    private final ILiteFlowExecuteService liteFlowExecuteService;
    private final IAiToolService aiToolService;
    private final LfChainCaseMapper lfChainCaseMapper;

    public LiteFlowChainAsToolService(ILfChainService lfChainService, ILiteFlowExecuteService liteFlowExecuteService,
            IAiToolService aiToolService, LfChainCaseMapper lfChainCaseMapper)
    {
        this.lfChainService = lfChainService;
        this.liteFlowExecuteService = liteFlowExecuteService;
        this.aiToolService = aiToolService;
        this.lfChainCaseMapper = lfChainCaseMapper;
    }

    public Map<String, Object> status(Long chainId)
    {
        LfChain chain = requireChain(chainId);
        AiTool tool = findTool(chain.getChainName());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("chainName", chain.getChainName());
        out.put("chainDesc", chain.getChainDesc());
        out.put("published", isPublished(chain));
        out.put("agentChain", liteFlowExecuteService.chainContainsAgent(chain.getChainName()));
        out.put("exposed", tool != null);
        out.put("toolCode", toolCodeOf(chain.getChainName()));
        out.put("exposeMcp", tool != null && StringUtils.isNotEmpty(tool.getMcpServerKey()));
        out.put("tool", tool);
        return out;
    }

    public AiTool expose(Long chainId, boolean exposeMcp, String schemaOverride, String username)
    {
        LfChain chain = requireChain(chainId);
        if (!isPublished(chain))
        {
            throw new ServiceException("请先发布链路后再设为工具");
        }
        boolean agentChain = liteFlowExecuteService.chainContainsAgent(chain.getChainName());
        if (exposeMcp && agentChain)
        {
            throw new ServiceException("含 Agent 的链路默认不暴露给开放 MCP，可仅作为后台智能体工具");
        }
        String toolCode = toolCodeOf(chain.getChainName());
        String schema = StringUtils.isNotEmpty(schemaOverride) ? schemaOverride.trim()
                : inferSchema(chain.getChainName());
        AiTool existing = findTool(chain.getChainName());
        if (existing == null)
        {
            existing = aiToolService.selectAiToolByCode(toolCode);
        }
        if (existing != null && !TOOL_TYPE.equalsIgnoreCase(existing.getToolType()))
        {
            throw new ServiceException("工具编码已被占用: " + existing.getToolCode());
        }
        if (existing == null)
        {
            AiTool tool = new AiTool();
            tool.setToolCode(toolCode);
            tool.setToolName(displayName(chain));
            tool.setToolType(TOOL_TYPE);
            tool.setDescription(descriptionOf(chain));
            tool.setInputSchemaJson(schema);
            tool.setInvokeKey(chain.getChainName());
            tool.setMcpServerKey(exposeMcp ? MCP_SERVER : "");
            tool.setEnabled("1");
            tool.setCreateBy(username);
            tool.setRemark("P1-4 链路工具 " + chain.getChainName());
            aiToolService.insertAiTool(tool);
            return aiToolService.selectAiToolByCode(toolCode);
        }
        existing.setToolName(displayName(chain));
        existing.setDescription(descriptionOf(chain));
        existing.setInputSchemaJson(schema);
        existing.setInvokeKey(chain.getChainName());
        existing.setMcpServerKey(exposeMcp ? MCP_SERVER : "");
        existing.setEnabled("1");
        existing.setUpdateBy(username);
        aiToolService.updateAiTool(existing);
        return aiToolService.selectAiToolById(existing.getId());
    }

    public void unexpose(Long chainId)
    {
        LfChain chain = requireChain(chainId);
        AiTool tool = findTool(chain.getChainName());
        if (tool == null)
        {
            return;
        }
        aiToolService.deleteAiToolByIds(new Long[] { tool.getId() });
    }

    public static String toolCodeOf(String chainName)
    {
        String raw = TOOL_CODE_PREFIX + (chainName == null ? "" : chainName);
        return raw.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private AiTool findTool(String chainName)
    {
        AiTool byInvoke = aiToolService.selectAiToolByTypeAndInvokeKey(TOOL_TYPE, chainName);
        if (byInvoke != null)
        {
            return byInvoke;
        }
        return aiToolService.selectAiToolByCode(toolCodeOf(chainName));
    }

    private LfChain requireChain(Long chainId)
    {
        if (chainId == null)
        {
            throw new ServiceException("链路 id 不能为空");
        }
        LfChain chain = lfChainService.selectLfChainById(chainId);
        if (chain == null)
        {
            throw new ServiceException("链路不存在");
        }
        return chain;
    }

    private static boolean isPublished(LfChain chain)
    {
        return !"1".equals(chain.getDraftFlag()) && !"1".equals(chain.getStatus());
    }

    private static String displayName(LfChain chain)
    {
        if (StringUtils.isNotEmpty(chain.getChainDesc()))
        {
            return "链路 " + chain.getChainDesc();
        }
        return "链路 " + chain.getChainName();
    }

    private static String descriptionOf(LfChain chain)
    {
        String desc = StringUtils.isEmpty(chain.getChainDesc()) ? chain.getChainName() : chain.getChainDesc();
        return "执行已发布链路 " + chain.getChainName() + "（" + desc + "）";
    }

    private String inferSchema(String chainName)
    {
        List<LfChainCase> cases = lfChainCaseMapper.selectEnabledByChainName(chainName);
        JSONObject properties = new JSONObject(new LinkedHashMap<>());
        if (cases != null)
        {
            for (LfChainCase item : cases)
            {
                JSONObject param = parseObject(item.getParamJson());
                if (param == null || param.isEmpty())
                {
                    continue;
                }
                for (String key : param.keySet())
                {
                    if (StringUtils.isEmpty(key) || properties.containsKey(key))
                    {
                        continue;
                    }
                    JSONObject prop = new JSONObject();
                    prop.put("type", jsonSchemaType(param.get(key)));
                    prop.put("description", "来自试跑用例");
                    properties.put(key, prop);
                }
            }
        }
        JSONObject root = new JSONObject();
        root.put("type", "object");
        root.put("properties", properties);
        root.put("required", new JSONArray());
        return root.toJSONString();
    }

    private static JSONObject parseObject(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            Object parsed = JSON.parse(json);
            return parsed instanceof JSONObject ? (JSONObject) parsed : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String jsonSchemaType(Object value)
    {
        if (value instanceof Boolean)
        {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short)
        {
            return "integer";
        }
        if (value instanceof Number)
        {
            return "number";
        }
        if (value instanceof JSONArray)
        {
            return "array";
        }
        if (value instanceof JSONObject)
        {
            return "object";
        }
        return "string";
    }
}
