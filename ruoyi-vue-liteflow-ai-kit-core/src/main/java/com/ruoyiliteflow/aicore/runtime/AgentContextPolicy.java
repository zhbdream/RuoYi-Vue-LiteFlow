package com.ruoyiliteflow.aicore.runtime;

/** 上下文策略（由 platform 装入 AgentDefinition） */
public class AgentContextPolicy
{
    private String policyCode;
    private int windowSize = 10;
    private boolean enableSummary;
    private String variableTemplate;

    public String getPolicyCode()
    {
        return policyCode;
    }

    public void setPolicyCode(String policyCode)
    {
        this.policyCode = policyCode;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int windowSize)
    {
        this.windowSize = windowSize;
    }

    public boolean isEnableSummary()
    {
        return enableSummary;
    }

    public void setEnableSummary(boolean enableSummary)
    {
        this.enableSummary = enableSummary;
    }

    public String getVariableTemplate()
    {
        return variableTemplate;
    }

    public void setVariableTemplate(String variableTemplate)
    {
        this.variableTemplate = variableTemplate;
    }
}
