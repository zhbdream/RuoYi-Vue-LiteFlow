package com.ruoyiliteflow.aicore.runtime;

import java.util.HashMap;
import java.util.Map;

public class AgentRunRequest
{
    private String message;
    private String principal;
    private String sessionId = "default";
    private Map<String, Object> variables = new HashMap<>();

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal(String principal)
    {
        this.principal = principal;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getVariables()
    {
        return variables;
    }

    public void setVariables(Map<String, Object> variables)
    {
        this.variables = variables != null ? variables : new HashMap<>();
    }
}
