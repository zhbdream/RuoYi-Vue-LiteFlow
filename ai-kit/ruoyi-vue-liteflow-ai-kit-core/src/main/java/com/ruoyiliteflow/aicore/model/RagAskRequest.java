package com.ruoyiliteflow.aicore.model;

public class RagAskRequest
{
    private String question;
    private String principal;
    private Integer maxResults;
    private Double minScore;

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal(String principal)
    {
        this.principal = principal;
    }

    public Integer getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults)
    {
        this.maxResults = maxResults;
    }

    public Double getMinScore()
    {
        return minScore;
    }

    public void setMinScore(Double minScore)
    {
        this.minScore = minScore;
    }
}
