package com.ruoyiliteflow.aikit.platform.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiTool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String toolCode;
    private String toolName;
    private String toolType;
    private String description;
    private String inputSchemaJson;
    private String invokeKey;
    private String mcpServerKey;
    private String enabled;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "工具编码不能为空")
    @Size(max = 64)
    public String getToolCode()
    {
        return toolCode;
    }

    public void setToolCode(String toolCode)
    {
        this.toolCode = toolCode;
    }

    @Size(max = 128)
    public String getToolName()
    {
        return toolName;
    }

    public void setToolName(String toolName)
    {
        this.toolName = toolName;
    }

    @NotBlank(message = "工具类型不能为空")
    public String getToolType()
    {
        return toolType;
    }

    public void setToolType(String toolType)
    {
        this.toolType = toolType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getInputSchemaJson()
    {
        return inputSchemaJson;
    }

    public void setInputSchemaJson(String inputSchemaJson)
    {
        this.inputSchemaJson = inputSchemaJson;
    }

    public String getInvokeKey()
    {
        return invokeKey;
    }

    public void setInvokeKey(String invokeKey)
    {
        this.invokeKey = invokeKey;
    }

    public String getMcpServerKey()
    {
        return mcpServerKey;
    }

    public void setMcpServerKey(String mcpServerKey)
    {
        this.mcpServerKey = mcpServerKey;
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
