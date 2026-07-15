package com.ruoyiliteflow.langchain.support;

/**
 * 运行时模型凭据（来自 lf_agent_model 或 yml 回退）
 */
public class Lc4jModelCredential
{
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;

    public Lc4jModelCredential(String apiKey, String baseUrl, String modelName)
    {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public String getModelName()
    {
        return modelName;
    }
}