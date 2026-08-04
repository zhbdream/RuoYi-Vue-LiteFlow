package com.ruoyiliteflow.langchain.domain;

/**
 * AI Kit 薄适配上下文：链路试跑传入 agentCode + message。
 */
public class AiKitAgentContext
{
    private String agentCode = "rag";
    private String message;
    private String principal;
    private String answer;
    private String model;

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

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

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }
}
