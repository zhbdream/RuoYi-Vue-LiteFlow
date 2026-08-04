package com.ruoyiliteflow.aicore.model;

public class RiskAnalyzeResult
{
    private String riskLevel;
    private String analysis;

    public RiskAnalyzeResult()
    {
    }

    public RiskAnalyzeResult(String riskLevel, String analysis)
    {
        this.riskLevel = riskLevel;
        this.analysis = analysis;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getAnalysis()
    {
        return analysis;
    }

    public void setAnalysis(String analysis)
    {
        this.analysis = analysis;
    }
}
