package com.ruoyiliteflow.aicore.runtime;

import java.util.ArrayList;
import java.util.List;

public class AgentRunResult
{
    private String agentCode;
    private String content;
    private String model;
    private List<Object> toolTrace = new ArrayList<>();

    public AgentRunResult()
    {
    }

    public AgentRunResult(String agentCode, String content, String model)
    {
        this.agentCode = agentCode;
        this.content = content;
        this.model = model;
    }

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public List<Object> getToolTrace()
    {
        return toolTrace;
    }

    public void setToolTrace(List<Object> toolTrace)
    {
        this.toolTrace = toolTrace != null ? toolTrace : new ArrayList<>();
    }
}
