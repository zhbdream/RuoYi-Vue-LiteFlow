package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.runtime.AgentContextPolicy;
import com.ruoyiliteflow.aicore.runtime.AgentDefinition;
import com.ruoyiliteflow.aicore.runtime.AgentDefinitionProvider;
import com.ruoyiliteflow.aicore.spi.ToolCatalog;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aikit.platform.domain.AiContextPolicy;
import com.ruoyiliteflow.aikit.platform.mapper.AiAgentMapper;
import com.ruoyiliteflow.aikit.platform.mapper.AiContextPolicyMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiModelService;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 从 ai_agent / ai_model / ai_tool / ai_skill / ai_context_policy 组装 AgentDefinition。
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class DbAgentDefinitionProvider implements AgentDefinitionProvider
{
    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private AiContextPolicyMapper aiContextPolicyMapper;

    @Autowired
    private IAiModelService aiModelService;

    @Autowired(required = false)
    private ToolCatalog toolCatalog;

    @Value("${ruoyi.ai.openai.api-key:}")
    private String ymlApiKey;

    @Value("${ruoyi.ai.openai.base-url:https://api.deepseek.com/v1}")
    private String ymlBaseUrl;

    @Value("${ruoyi.ai.openai.model:deepseek-chat}")
    private String ymlModel;

    @Override
    public AgentDefinition load(String agentCode)
    {
        if (StringUtils.isEmpty(agentCode))
        {
            return null;
        }
        AiAgent agent = aiAgentMapper.selectAiAgentByCode(agentCode.trim());
        if (agent == null)
        {
            return null;
        }
        AgentDefinition def = new AgentDefinition();
        def.setAgentCode(agent.getAgentCode());
        def.setAgentName(agent.getAgentName());
        def.setSystemPrompt(agent.getSystemPrompt());
        def.setEnabled("1".equals(agent.getEnabled()));
        if (agent.getTemperature() != null)
        {
            def.setTemperature(agent.getTemperature().doubleValue());
        }

        List<String> toolCodes = aiAgentMapper.selectToolCodesByAgentId(agent.getId());
        def.setToolCodes(toolCodes);
        if (toolCatalog != null && toolCodes != null && !toolCodes.isEmpty())
        {
            List<ToolDescriptor> tools = toolCatalog.resolve(toolCodes);
            def.setTools(tools);
        }

        List<String> knowledgeCodes = aiAgentMapper.selectKnowledgeCodesByAgentId(agent.getId());
        def.setKnowledgeCodes(knowledgeCodes);

        List<String> skillCodes = aiAgentMapper.selectSkillCodesByAgentId(agent.getId());
        def.setSkillCodes(skillCodes);

        def.setContextPolicy(resolveContextPolicy(agent.getContextPolicyId()));

        AiModelCredential cred = aiModelService.resolveRuntimeById(agent.getModelId());
        if (cred == null)
        {
            cred = aiModelService.resolveRuntimeDefault();
        }
        if (cred == null && StringUtils.isNotEmpty(ymlApiKey))
        {
            cred = new AiModelCredential(ymlApiKey, ymlBaseUrl, ymlModel);
        }
        def.setCredential(cred);
        return def;
    }

    private AgentContextPolicy resolveContextPolicy(Long contextPolicyId)
    {
        AiContextPolicy policy = null;
        if (contextPolicyId != null && contextPolicyId > 0)
        {
            policy = aiContextPolicyMapper.selectAiContextPolicyById(contextPolicyId);
        }
        if (policy == null || !"1".equals(policy.getEnabled()))
        {
            policy = aiContextPolicyMapper.selectDefaultEnabled();
        }
        if (policy == null)
        {
            return null;
        }
        AgentContextPolicy runtime = new AgentContextPolicy();
        runtime.setPolicyCode(policy.getPolicyCode());
        runtime.setWindowSize(policy.getWindowSize() != null ? policy.getWindowSize() : 10);
        runtime.setEnableSummary("1".equals(policy.getEnableSummary()));
        runtime.setVariableTemplate(policy.getVariableTemplate());
        return runtime;
    }
}
