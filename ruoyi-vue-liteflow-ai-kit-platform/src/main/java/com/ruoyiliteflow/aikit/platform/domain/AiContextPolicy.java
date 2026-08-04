package com.ruoyiliteflow.aikit.platform.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiContextPolicy extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String policyCode;
    private String policyName;
    private Integer windowSize;
    private String enableSummary;
    private String variableTemplate;
    private String isDefault;
    private String enabled;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "策略编码不能为空")
    @Size(max = 64)
    public String getPolicyCode()
    {
        return policyCode;
    }

    public void setPolicyCode(String policyCode)
    {
        this.policyCode = policyCode;
    }

    @Size(max = 128)
    public String getPolicyName()
    {
        return policyName;
    }

    public void setPolicyName(String policyName)
    {
        this.policyName = policyName;
    }

    public Integer getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(Integer windowSize)
    {
        this.windowSize = windowSize;
    }

    public String getEnableSummary()
    {
        return enableSummary;
    }

    public void setEnableSummary(String enableSummary)
    {
        this.enableSummary = enableSummary;
    }

    @Size(max = 512)
    public String getVariableTemplate()
    {
        return variableTemplate;
    }

    public void setVariableTemplate(String variableTemplate)
    {
        this.variableTemplate = variableTemplate;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}
