package com.ruoyiliteflow.web.aikit;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aicore.spi.LocalToolHandler;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;
import com.ruoyiliteflow.web.service.LiteFlowChainAsToolService;

/**
 * admin 进程内执行 liteflow-chain 工具，不依赖独立 MCP :8090。
 * 权限复用链路级「可执行」。
 */
@Component
public class LiteFlowChainLocalToolHandler implements LocalToolHandler
{
    private final ILiteFlowExecuteService liteFlowExecuteService;

    public LiteFlowChainLocalToolHandler(ILiteFlowExecuteService liteFlowExecuteService)
    {
        this.liteFlowExecuteService = liteFlowExecuteService;
    }

    @Override
    public boolean supports(ToolDescriptor tool)
    {
        if (tool == null)
        {
            return false;
        }
        if (LiteFlowChainAsToolService.TOOL_TYPE.equalsIgnoreCase(tool.getToolType()))
        {
            return true;
        }
        return LiteFlowChainAsToolService.MCP_SERVER.equals(tool.getMcpServerKey());
    }

    @Override
    public String execute(ToolDescriptor tool, String argumentsJson)
    {
        String chainName = resolveChainName(tool);
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("source", "admin-local");
        wrap.put("chainName", chainName);
        if (StringUtils.isEmpty(chainName))
        {
            wrap.put("ok", false);
            wrap.put("error", "缺少链路名");
            return JSON.toJSONString(wrap);
        }
        try
        {
            JSONObject args = parseArgs(argumentsJson);
            LiteFlowExecuteResultVo result = liteFlowExecuteService.execute(
                    chainName, args, currentUser(), false);
            wrap.put("ok", result != null && result.isSuccess());
            if (result != null)
            {
                wrap.put("requestId", result.getRequestId());
                wrap.put("executeStepStr", result.getExecuteStepStr());
                wrap.put("message", result.getMessage());
                wrap.put("logId", result.getLogId());
                if (result.getContextData() != null && !result.getContextData().isEmpty())
                {
                    wrap.put("contextData", result.getContextData());
                }
            }
            return JSON.toJSONString(wrap);
        }
        catch (Exception ex)
        {
            wrap.put("ok", false);
            wrap.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return JSON.toJSONString(wrap);
        }
    }

    private static String resolveChainName(ToolDescriptor tool)
    {
        if (StringUtils.isNotEmpty(tool.getInvokeKey()))
        {
            return tool.getInvokeKey();
        }
        String code = tool.getToolCode();
        if (StringUtils.isNotEmpty(code) && code.startsWith(LiteFlowChainAsToolService.TOOL_CODE_PREFIX))
        {
            return code.substring(LiteFlowChainAsToolService.TOOL_CODE_PREFIX.length());
        }
        return code;
    }

    private static JSONObject parseArgs(String argumentsJson)
    {
        if (StringUtils.isEmpty(argumentsJson))
        {
            return new JSONObject();
        }
        try
        {
            JSONObject obj = JSON.parseObject(argumentsJson);
            return obj == null ? new JSONObject() : obj;
        }
        catch (Exception ex)
        {
            return new JSONObject();
        }
    }

    private static String currentUser()
    {
        try
        {
            String name = SecurityUtils.getUsername();
            return StringUtils.isEmpty(name) ? "ai-agent" : name;
        }
        catch (Exception e)
        {
            return "ai-agent";
        }
    }
}
