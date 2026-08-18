package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;

/** 一次 Agent 调用的观测记录（由 platform 落库，core 只传结构） */
public class AgentRunLog
{
    private String agentCode;
    private String sessionId;
    private String principal;
    private String model;
    private long costMs;
    private boolean kbHit;
    private String userMessage;
    private String errorMsg;
    private List<Object> toolTrace = Collections.emptyList();

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

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public long getCostMs()
    {
        return costMs;
    }

    public void setCostMs(long costMs)
    {
        this.costMs = costMs;
    }

    public boolean isKbHit()
    {
        return kbHit;
    }

    public void setKbHit(boolean kbHit)
    {
        this.kbHit = kbHit;
    }

    public String getUserMessage()
    {
        return userMessage;
    }

    public void setUserMessage(String userMessage)
    {
        this.userMessage = userMessage;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public List<Object> getToolTrace()
    {
        return toolTrace;
    }

    public void setToolTrace(List<Object> toolTrace)
    {
        this.toolTrace = toolTrace != null ? toolTrace : Collections.emptyList();
    }
}
