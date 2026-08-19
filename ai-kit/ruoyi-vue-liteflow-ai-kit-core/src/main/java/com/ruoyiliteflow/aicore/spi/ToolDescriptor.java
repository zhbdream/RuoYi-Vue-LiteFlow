package com.ruoyiliteflow.aicore.spi;

import java.util.Map;

/** 工具元信息（供运行时拼入 prompt / MCP 动态暴露） */
public class ToolDescriptor
{
    private String toolCode;
    private String toolName;
    private String toolType;
    private String description;
    private String invokeKey;
    private String mcpServerKey;

    /** JSON Schema，供模型填参 */
    private String inputSchemaJson;

    public String getToolCode()
    {
        return toolCode;
    }

    public void setToolCode(String toolCode)
    {
        this.toolCode = toolCode;
    }

    public String getToolName()
    {
        return toolName;
    }

    public void setToolName(String toolName)
    {
        this.toolName = toolName;
    }

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

    public String getInputSchemaJson()
    {
        return inputSchemaJson;
    }

    public void setInputSchemaJson(String inputSchemaJson)
    {
        this.inputSchemaJson = inputSchemaJson;
    }

    public Map<String, Object> toMcpToolMap()
    {
        return Map.of(
                "name", toolCode == null ? "" : toolCode,
                "description", description == null ? (toolName == null ? "" : toolName) : description,
                "server", mcpServerKey == null ? "ai-core" : mcpServerKey,
                "dynamic", true);
    }
}
