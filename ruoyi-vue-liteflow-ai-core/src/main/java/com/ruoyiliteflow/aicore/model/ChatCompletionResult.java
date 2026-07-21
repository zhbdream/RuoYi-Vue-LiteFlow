package com.ruoyiliteflow.aicore.model;

public class ChatCompletionResult
{
    private String content;
    private String model;

    public ChatCompletionResult()
    {
    }

    public ChatCompletionResult(String content, String model)
    {
        this.content = content;
        this.model = model;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
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
