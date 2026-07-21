package com.ruoyiliteflow.aicore.model;

/**
 * LLM 运行时凭证（仅内存使用，禁止写入日志）
 */
public class AiModelCredential
{
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;

    public AiModelCredential(String apiKey, String baseUrl, String modelName)
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
