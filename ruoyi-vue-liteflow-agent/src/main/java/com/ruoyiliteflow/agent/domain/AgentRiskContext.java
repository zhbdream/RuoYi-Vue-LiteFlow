package com.ruoyiliteflow.agent.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Demo7 Agent 风控链路上下文
 */
public class AgentRiskContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String userType;
    private BigDecimal amount;
    private String scene;
    private String riskLevel;
    private String agentReply;
    private boolean prepared;
    private boolean notified;

    public String getOrderId()
    {
        return orderId;
    }

    public void setOrderId(String orderId)
    {
        this.orderId = orderId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserType()
    {
        return userType;
    }

    public void setUserType(String userType)
    {
        this.userType = userType;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getAgentReply()
    {
        return agentReply;
    }

    public void setAgentReply(String agentReply)
    {
        this.agentReply = agentReply;
    }

    public boolean isPrepared()
    {
        return prepared;
    }

    public void setPrepared(boolean prepared)
    {
        this.prepared = prepared;
    }

    public boolean isNotified()
    {
        return notified;
    }

    public void setNotified(boolean notified)
    {
        this.notified = notified;
    }
}
