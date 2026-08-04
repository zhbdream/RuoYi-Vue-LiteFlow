package com.ruoyiliteflow.aicore.runtime;

import java.util.ArrayList;
import java.util.List;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;

/**
 * 智能体运行时定义（由 platform / yml 组装后注入，core 不依赖 platform）
 */
public class AgentDefinition
{
    private String agentCode;
    private String agentName;
    private String systemPrompt;
    private Double temperature;
    private boolean enabled = true;
    private AiModelCredential credential;
    private List<String> toolCodes = new ArrayList<>();
    private List<String> knowledgeCodes = new ArrayList<>();
    private List<String> skillCodes = new ArrayList<>();
    private List<ToolDescriptor> tools = new ArrayList<>();
    private AgentContextPolicy contextPolicy;

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

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

    public Double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(Double temperature)
    {
        this.temperature = temperature;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public AiModelCredential getCredential()
    {
        return credential;
    }

    public void setCredential(AiModelCredential credential)
    {
        this.credential = credential;
    }

    public List<String> getToolCodes()
    {
        return toolCodes;
    }

    public void setToolCodes(List<String> toolCodes)
    {
        this.toolCodes = toolCodes != null ? toolCodes : new ArrayList<>();
    }

    public List<String> getKnowledgeCodes()
    {
        return knowledgeCodes;
    }

    public void setKnowledgeCodes(List<String> knowledgeCodes)
    {
        this.knowledgeCodes = knowledgeCodes != null ? knowledgeCodes : new ArrayList<>();
    }

    public List<String> getSkillCodes()
    {
        return skillCodes;
    }

    public void setSkillCodes(List<String> skillCodes)
    {
        this.skillCodes = skillCodes != null ? skillCodes : new ArrayList<>();
    }

    public List<ToolDescriptor> getTools()
    {
        return tools;
    }

    public void setTools(List<ToolDescriptor> tools)
    {
        this.tools = tools != null ? tools : new ArrayList<>();
    }

    public AgentContextPolicy getContextPolicy()
    {
        return contextPolicy;
    }

    public void setContextPolicy(AgentContextPolicy contextPolicy)
    {
        this.contextPolicy = contextPolicy;
    }
}
