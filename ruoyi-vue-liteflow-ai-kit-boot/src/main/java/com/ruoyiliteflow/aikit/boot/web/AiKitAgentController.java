package com.ruoyiliteflow.aikit.boot.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aicore.facade.IAiChatFacade;
import com.ruoyiliteflow.aicore.facade.IAiRagFacade;
import com.ruoyiliteflow.aicore.facade.IAiRiskFacade;
import com.ruoyiliteflow.aicore.model.ChatCompletionRequest;
import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunResult;
import com.ruoyiliteflow.aicore.runtime.AgentRuntime;
import com.ruoyiliteflow.aikit.boot.client.McpToolClient;
import com.ruoyiliteflow.aikit.boot.config.AiKitBootSecurityConfig.AiKitBootProperties;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 合并后的 Agent API：配置驱动 /{agentCode}/run + 兼容 chat|risk|rag|ops
 */
@RestController
@RequestMapping("/agent")
public class AiKitAgentController
{
    private final AiKitBootProperties properties;
    private final McpToolClient mcpClient;
    private final IAiChatFacade chatFacade;
    private final IAiRiskFacade riskFacade;
    private final IAiRagFacade ragFacade;

    @Autowired(required = false)
    private AgentRuntime agentRuntime;

    public AiKitAgentController(AiKitBootProperties properties, McpToolClient mcpClient, IAiChatFacade chatFacade,
            IAiRiskFacade riskFacade, IAiRagFacade ragFacade)
    {
        this.properties = properties;
        this.mcpClient = mcpClient;
        this.chatFacade = chatFacade;
        this.riskFacade = riskFacade;
        this.ragFacade = ragFacade;
    }

    @GetMapping("/health")
    public AjaxResult health()
    {
        return AjaxResult.success(Map.of(
                "service", "ai-kit-boot",
                "useMcp", properties.isUseMcp(),
                "mcpBaseUrl", properties.getMcpBaseUrl(),
                "agentRuntime", agentRuntime != null,
                "agents", List.of("chat", "risk", "rag", "ops")));
    }

    @PostMapping("/{agentCode}/run")
    public AjaxResult runConfigured(@PathVariable String agentCode, @RequestBody JSONObject body)
    {
        if (agentRuntime == null)
        {
            throw new ServiceException("AgentRuntime 未就绪");
        }
        AgentRunRequest req = new AgentRunRequest();
        req.setMessage(firstNonEmpty(body.getString("message"), body.getString("userMessage")));
        req.setPrincipal(firstNonEmpty(body.getString("principal"), "boot"));
        req.setSessionId(firstNonEmpty(body.getString("sessionId"), "default"));
        if (body.get("variables") instanceof Map<?, ?> vars)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) vars;
            req.setVariables(map);
        }
        AgentRunResult result = agentRuntime.invoke(agentCode, req);
        return AjaxResult.success(result);
    }

    @GetMapping("/mcp/tools")
    public AjaxResult mcpTools()
    {
        return AjaxResult.success(mcpClient.listAiCoreTools());
    }

    @PostMapping("/chat/complete")
    public AjaxResult chatComplete(@RequestBody JSONObject body)
    {
        String userMessage = firstNonEmpty(body.getString("userMessage"), body.getString("message"));
        String principal = firstNonEmpty(body.getString("principal"), "agent-chat");
        String systemPrompt = firstNonEmpty(body.getString("systemPrompt"), properties.getChatSystemPrompt());
        if (properties.isUseMcp())
        {
            Map<String, Object> args = new HashMap<>();
            args.put("systemPrompt", systemPrompt);
            args.put("userMessage", userMessage);
            args.put("principal", principal);
            return AjaxResult.success(mcpClient.callAiCore("chat_completion", args));
        }
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setSystemPrompt(systemPrompt);
        req.setUserMessage(userMessage);
        req.setPrincipal(principal);
        return AjaxResult.success(chatFacade.complete(req));
    }

    @PostMapping("/risk/analyze")
    public AjaxResult riskAnalyze(@RequestBody JSONObject body)
    {
        if (StringUtils.isEmpty(body.getString("principal")))
        {
            body.put("principal", "agent-risk");
        }
        if (properties.isUseMcp())
        {
            return AjaxResult.success(mcpClient.callAiCore("risk_analyze", new HashMap<>(body)));
        }
        return AjaxResult.success(riskFacade.analyze(body.to(RiskAnalyzeRequest.class)));
    }

    @PostMapping("/rag/ask")
    public AjaxResult ragAsk(@RequestBody JSONObject body)
    {
        if (StringUtils.isEmpty(body.getString("principal")))
        {
            body.put("principal", "agent-rag");
        }
        if (properties.isUseMcp())
        {
            return AjaxResult.success(mcpClient.callAiCore("rag_ask", new HashMap<>(body)));
        }
        return AjaxResult.success(ragFacade.ask(body.to(RagAskRequest.class)));
    }

    @PostMapping("/ops/chat")
    public AjaxResult opsChat(@RequestBody JSONObject body)
    {
        String question = firstNonEmpty(body.getString("userMessage"), body.getString("message"));
        JSONArray chains = mcpClient.listChains();
        JSONObject dashboard = mcpClient.dashboard();
        String facts = "【链路列表】\n" + JSON.toJSONString(chains)
                + "\n\n【监控摘要】\n" + JSON.toJSONString(dashboard);
        String userMessage = facts + "\n\n【用户问题】\n" + question;
        JSONObject reply = mcpClient.callAiCore("chat_completion", Map.of(
                "systemPrompt", properties.getOpsSystemPrompt(),
                "userMessage", userMessage,
                "principal", "agent-ops"));
        return AjaxResult.success(Map.of(
                "facts", Map.of("chains", chains, "dashboard", dashboard),
                "reply", reply));
    }

    private static String firstNonEmpty(String a, String b)
    {
        return StringUtils.isNotEmpty(a) ? a : b;
    }
}
