package com.ruoyiliteflow.agentchat.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.agentchat.client.McpAiCoreClient;
import com.ruoyiliteflow.agentchat.config.AgentChatProperties;
import com.ruoyiliteflow.aicore.facade.IAiChatFacade;
import com.ruoyiliteflow.aicore.model.ChatCompletionRequest;
import com.ruoyiliteflow.aicore.model.ChatCompletionResult;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/agent/chat")
public class AgentChatController
{
    private final AgentChatProperties properties;
    private final McpAiCoreClient mcpClient;
    private final IAiChatFacade chatFacade;

    public AgentChatController(AgentChatProperties properties, McpAiCoreClient mcpClient, IAiChatFacade chatFacade)
    {
        this.properties = properties;
        this.mcpClient = mcpClient;
        this.chatFacade = chatFacade;
    }

    @GetMapping("/health")
    public AjaxResult health()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "agent-chat");
        data.put("useMcp", properties.isUseMcp());
        data.put("mcpBaseUrl", properties.getMcpBaseUrl());
        return AjaxResult.success(data);
    }

    @GetMapping("/mcp/tools")
    public AjaxResult mcpTools()
    {
        return AjaxResult.success(mcpClient.listTools());
    }

    /**
     * 单轮对话。默认经 MCP chat_completion；useMcp=false 时走本地 Facade。
     */
    @PostMapping("/complete")
    public AjaxResult complete(@RequestBody JSONObject body)
    {
        String userMessage = body.getString("userMessage");
        if (StringUtils.isEmpty(userMessage))
        {
            userMessage = body.getString("message");
        }
        String principal = body.getString("principal");
        if (StringUtils.isEmpty(principal))
        {
            principal = "agent-chat";
        }
        String systemPrompt = body.getString("systemPrompt");
        if (StringUtils.isEmpty(systemPrompt))
        {
            systemPrompt = properties.getSystemPrompt();
        }

        if (properties.isUseMcp())
        {
            Map<String, Object> args = new HashMap<>();
            args.put("systemPrompt", systemPrompt);
            args.put("userMessage", userMessage);
            args.put("principal", principal);
            JSONObject data = mcpClient.callTool("chat_completion", args);
            return AjaxResult.success(data);
        }

        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setSystemPrompt(systemPrompt);
        req.setUserMessage(userMessage);
        req.setPrincipal(principal);
        ChatCompletionResult result = chatFacade.complete(req);
        return AjaxResult.success(result);
    }
}
