package com.ruoyiliteflow.aikit.platform.domain;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiAgent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String agentCode;
    private String agentName;
    private String systemPrompt;
    private Long modelId;
    private BigDecimal temperature;
    private Long contextPolicyId;
    private String enabled;

    /** 非持久化：绑定工具 ID 列表 */
    private List<Long> toolIds;

    /** 非持久化：绑定工具编码（查询回填） */
    private List<String> toolCodes;

    /** 非持久化：绑定知识库 ID */
    private List<Long> kbIds;

    /** 非持久化：绑定知识库编码 */
    private List<String> knowledgeCodes;

    /** 非持久化：绑定技能 ID 列表 */
    private List<Long> skillIds;

    /** 非持久化：绑定技能编码（查询回填） */
    private List<String> skillCodes;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 64)
    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    @Size(max = 128)
    public String getAgentName()
    {
        return agentName;
    }

    public void setAgentName(String agentName)
    {
        this.agentName = agentName;
    }

    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt)
    {
        this.systemPrompt = systemPrompt;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    public BigDecimal getTemperature()
    {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature)
    {
        this.temperature = temperature;
    }

    public Long getContextPolicyId()
    {
        return contextPolicyId;
    }

    public void setContextPolicyId(Long contextPolicyId)
    {
        this.contextPolicyId = contextPolicyId;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }

    public List<Long> getToolIds()
    {
        return toolIds;
    }

    public void setToolIds(List<Long> toolIds)
    {
        this.toolIds = toolIds;
    }

    public List<String> getToolCodes()
    {
        return toolCodes;
    }

    public void setToolCodes(List<String> toolCodes)
    {
        this.toolCodes = toolCodes;
    }

    public List<Long> getKbIds()
    {
        return kbIds;
    }

    public void setKbIds(List<Long> kbIds)
    {
        this.kbIds = kbIds;
    }

    public List<String> getKnowledgeCodes()
    {
        return knowledgeCodes;
    }

    public void setKnowledgeCodes(List<String> knowledgeCodes)
    {
        this.knowledgeCodes = knowledgeCodes;
    }

    public List<Long> getSkillIds()
    {
        return skillIds;
    }

    public void setSkillIds(List<Long> skillIds)
    {
        this.skillIds = skillIds;
    }

    public List<String> getSkillCodes()
    {
        return skillCodes;
    }

    public void setSkillCodes(List<String> skillCodes)
    {
        this.skillCodes = skillCodes;
    }
}
