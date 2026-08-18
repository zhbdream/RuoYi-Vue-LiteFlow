package com.ruoyiliteflow.aikit.platform.domain;

import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiAgentRunLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String agentCode;
    private String sessionId;
    private String principal;
    private String model;
    private Integer costMs;
    private String kbHit;
    private String toolHit;
    private String toolTrace;
    private String errorMsg;
    private String userMessage;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public Integer getCostMs()
    {
        return costMs;
    }

    public void setCostMs(Integer costMs)
    {
        this.costMs = costMs;
    }

    public String getKbHit()
    {
        return kbHit;
    }

    public void setKbHit(String kbHit)
    {
        this.kbHit = kbHit;
    }

    public String getToolHit()
    {
        return toolHit;
    }

    public void setToolHit(String toolHit)
    {
        this.toolHit = toolHit;
    }

    public String getToolTrace()
    {
        return toolTrace;
    }

    public void setToolTrace(String toolTrace)
    {
        this.toolTrace = toolTrace;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public String getUserMessage()
    {
        return userMessage;
    }

    public void setUserMessage(String userMessage)
    {
        this.userMessage = userMessage;
    }
}
