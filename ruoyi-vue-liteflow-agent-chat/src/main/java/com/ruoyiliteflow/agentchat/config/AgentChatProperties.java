package com.ruoyiliteflow.agentchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ruoyi.agent.chat")
public class AgentChatProperties
{
    /** 是否优先走 MCP HTTP（true）还是本地 Facade（false） */
    private boolean useMcp = true;
    private String mcpBaseUrl = "http://localhost:8090";
    private String mcpApiKey = "ruoyi-mcp-key-change-me";
    private String mcpHeaderName = "X-MCP-Api-Key";
    private String systemPrompt = "你是 RuoYi-Vue-LiteFlow 内部助手，回答简洁、准确，使用中文。";

    public boolean isUseMcp()
    {
        return useMcp;
    }

    public void setUseMcp(boolean useMcp)
    {
        this.useMcp = useMcp;
    }

    public String getMcpBaseUrl()
    {
        return mcpBaseUrl;
    }

    public void setMcpBaseUrl(String mcpBaseUrl)
    {
        this.mcpBaseUrl = mcpBaseUrl;
    }

    public String getMcpApiKey()
    {
        return mcpApiKey;
    }

    public void setMcpApiKey(String mcpApiKey)
    {
        this.mcpApiKey = mcpApiKey;
    }

    public String getMcpHeaderName()
    {
        return mcpHeaderName;
    }

    public void setMcpHeaderName(String mcpHeaderName)
    {
        this.mcpHeaderName = mcpHeaderName;
    }

    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt)
    {
        this.systemPrompt = systemPrompt;
    }
}
