package com.ruoyiliteflow.langchain.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * LangChain4j / LangGraph4j Demo 风控链路上下文
 */
public class Lc4jRiskContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String userType;
    private BigDecimal amount;
    private String scene;
    private String riskLevel;
    private String agentReply;
    private String graphTrace;
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

    public String getGraphTrace()
    {
        return graphTrace;
    }

    public void setGraphTrace(String graphTrace)
    {
        this.graphTrace = graphTrace;
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