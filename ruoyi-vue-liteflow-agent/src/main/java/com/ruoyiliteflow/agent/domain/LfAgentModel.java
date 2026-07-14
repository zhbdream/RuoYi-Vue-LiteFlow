package com.ruoyiliteflow.agent.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * Agent 模型配置 lf_agent_model
 */
public class LfAgentModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String modelCode;

    private String modelName;

    /** deepseek / openai-compatible */
    private String provider;

    /** openai-compatible map key，默认 deepseek */
    private String configKey;

    private String baseUrl;

    private String model;

    /** DB 中加密串，接口层不回传 */
    private String apiKeyEnc;

    /** 写入用明文 Key，不落库；查询不返回 */
    private String apiKey;

    /** 脱敏展示 */
    private String apiKeyMasked;

    private Boolean hasApiKey;

    /** 0正常 1停用 */
    private String status;

    /** 1默认 0否 */
    private String isDefault;

    private Integer dailyCallLimit;

    private Integer dailyTokenLimit;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "模型编码不能为空")
    @Size(max = 64, message = "模型编码长度不能超过64")
    public String getModelCode()
    {
        return modelCode;
    }

    public void setModelCode(String modelCode)
    {
        this.modelCode = modelCode;
    }

    @Size(max = 128, message = "名称长度不能超过128")
    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    @NotBlank(message = "供应商不能为空")
    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    @NotBlank(message = "configKey 不能为空")
    public String getConfigKey()
    {
        return configKey;
    }

    public void setConfigKey(String configKey)
    {
        this.configKey = configKey;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    @NotBlank(message = "模型名不能为空")
    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getApiKeyEnc()
    {
        return apiKeyEnc;
    }

    public void setApiKeyEnc(String apiKeyEnc)
    {
        this.apiKeyEnc = apiKeyEnc;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getApiKeyMasked()
    {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked)
    {
        this.apiKeyMasked = apiKeyMasked;
    }

    public Boolean getHasApiKey()
    {
        return hasApiKey;
    }

    public void setHasApiKey(Boolean hasApiKey)
    {
        this.hasApiKey = hasApiKey;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public Integer getDailyCallLimit()
    {
        return dailyCallLimit;
    }

    public void setDailyCallLimit(Integer dailyCallLimit)
    {
        this.dailyCallLimit = dailyCallLimit;
    }

    public Integer getDailyTokenLimit()
    {
        return dailyTokenLimit;
    }

    public void setDailyTokenLimit(Integer dailyTokenLimit)
    {
        this.dailyTokenLimit = dailyTokenLimit;
    }
}
