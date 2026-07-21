package com.ruoyiliteflow.mcp.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aicore.facade.IAiChatFacade;
import com.ruoyiliteflow.aicore.facade.IAiModelFacade;
import com.ruoyiliteflow.aicore.facade.IAiRagFacade;
import com.ruoyiliteflow.aicore.facade.IAiRiskFacade;
import com.ruoyiliteflow.aicore.model.AiModelInfo;
import com.ruoyiliteflow.aicore.model.ChatCompletionRequest;
import com.ruoyiliteflow.aicore.model.ChatCompletionResult;
import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.aicore.model.RagAskResult;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeResult;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.mcp.config.McpServerProperties;

/**
 * mcp-ai-core Tool 注册与调用
 */
@Component
public class AiCoreMcpTools
{
    public static final String SERVER = "ai-core";

    private final IAiModelFacade modelFacade;
    private final IAiChatFacade chatFacade;
    private final IAiRiskFacade riskFacade;
    private final IAiRagFacade ragFacade;
    private final McpServerProperties properties;

    public AiCoreMcpTools(IAiModelFacade modelFacade, IAiChatFacade chatFacade, IAiRiskFacade riskFacade,
            IAiRagFacade ragFacade, McpServerProperties properties)
    {
        this.modelFacade = modelFacade;
        this.chatFacade = chatFacade;
        this.riskFacade = riskFacade;
        this.ragFacade = ragFacade;
        this.properties = properties;
    }

    public boolean enabled()
    {
        return properties.isEnabled() && properties.getServers().isAiCore();
    }

    public List<Map<String, Object>> listTools()
    {
        return List.of(
                tool("list_models", "列出可用模型元信息（脱敏，不含 API Key）"),
                tool("get_default_model", "获取默认模型元信息"),
                tool("chat_completion", "单轮 Chat 补全。参数: systemPrompt?, userMessage, principal?"),
                tool("risk_analyze", "支付前风控研判。参数: orderId, userId?, userType?, amount?, scene?, principal?"),
                tool("rag_ask", "售后知识问答。参数: question, principal?, maxResults?, minScore?"),
                tool("quota_status", "返回配额说明（当前为占位，完整配额需接入 Redis Guard）"));
    }

    public Object call(String toolName, JSONObject args)
    {
        if (!enabled())
        {
            throw new ServiceException("mcp-ai-core 未启用");
        }
        if (args == null)
        {
            args = new JSONObject();
        }
        return switch (toolName)
        {
            case "list_models" -> modelFacade.listModels();
            case "get_default_model" -> modelFacade.getDefaultModel();
            case "chat_completion" -> chatCompletion(args);
            case "risk_analyze" -> riskAnalyze(args);
            case "rag_ask" -> ragAsk(args);
            case "quota_status" -> Map.of(
                    "enabled", true,
                    "note", "独立进程默认无 Redis 配额；接入 AiQuotaGuard 后生效",
                    "dimensions", List.of("agent:chat", "agent:risk", "agent:rag"));
            default -> throw new ServiceException("未知 Tool: " + toolName);
        };
    }

    private ChatCompletionResult chatCompletion(JSONObject args)
    {
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setSystemPrompt(args.getString("systemPrompt"));
        req.setUserMessage(args.getString("userMessage"));
        req.setPrincipal(args.getString("principal"));
        if (args.getDouble("temperature") != null)
        {
            req.setTemperature(args.getDouble("temperature"));
        }
        return chatFacade.complete(req);
    }

    private RiskAnalyzeResult riskAnalyze(JSONObject args)
    {
        RiskAnalyzeRequest req = args.to(RiskAnalyzeRequest.class);
        return riskFacade.analyze(req);
    }

    private RagAskResult ragAsk(JSONObject args)
    {
        RagAskRequest req = args.to(RagAskRequest.class);
        return ragFacade.ask(req);
    }

    private static Map<String, Object> tool(String name, String description)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("server", SERVER);
        return m;
    }

    public String toJson(Object value)
    {
        return JSON.toJSONString(value);
    }
}
