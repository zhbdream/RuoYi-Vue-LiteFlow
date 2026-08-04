package com.ruoyiliteflow.aicore.spi;

public class MemoryItem
{
    private String agentCode;
    private String sessionId;
    private String principal;
    private String memoryType;
    private String role;
    private String content;

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal(String principal)
    {
        this.principal = principal;
    }

    public String getMemoryType()
    {
        return memoryType;
    }

    public void setMemoryType(String memoryType)
    {
        this.memoryType = memoryType;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
}
