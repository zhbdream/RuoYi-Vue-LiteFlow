package com.ruoyiliteflow.aicore.model;

/**
 * 对外模型元信息（脱敏，不含 API Key）
 */
public class AiModelInfo
{
    private String modelCode;
    private String modelName;
    private String baseUrl;
    private String model;
    private boolean defaultModel;
    private boolean enabled;

    public String getModelCode()
    {
        return modelCode;
    }

    public void setModelCode(String modelCode)
    {
        this.modelCode = modelCode;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public boolean isDefaultModel()
    {
        return defaultModel;
    }

    public void setDefaultModel(boolean defaultModel)
    {
        this.defaultModel = defaultModel;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
