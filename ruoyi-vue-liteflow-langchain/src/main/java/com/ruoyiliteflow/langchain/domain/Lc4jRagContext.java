package com.ruoyiliteflow.langchain.domain;

import java.io.Serializable;

/**
 * LangChain4j RAG Demo 上下文（售后知识问答）
 */
public class Lc4jRagContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String question;
    private String answer;
    private String retrievedContext;
    private int hitCount;
    private boolean prepared;
    private boolean notified;

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

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