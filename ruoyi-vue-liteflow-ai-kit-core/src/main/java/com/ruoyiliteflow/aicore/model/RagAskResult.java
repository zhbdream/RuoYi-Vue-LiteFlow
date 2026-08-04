package com.ruoyiliteflow.aicore.model;

public class RagAskResult
{
    private String answer;
    private String retrievedContext;
    private int hitCount;

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getRetrievedContext()
    {
        return retrievedContext;
    }

    public void setRetrievedContext(String retrievedContext)
    {
        this.retrievedContext = retrievedContext;
    }

    public int getHitCount()
    {
        return hitCount;
    }

    public void setHitCount(int hitCount)
    {
        this.hitCount = hitCount;
    }
}
