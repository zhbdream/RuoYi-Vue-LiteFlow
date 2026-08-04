package com.ruoyiliteflow.aicore.runtime;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 无 DB / platform 时的内存定义，保证 {@code /agent/{code}/run} 可用。
 */
@Component
@ConditionalOnMissingBean(AgentDefinitionProvider.class)
public class FallbackAgentDefinitionProvider implements AgentDefinitionProvider
{
    private final Map<String, String> prompts;

    public FallbackAgentDefinitionProvider(
            @Value("${ruoyi.ai-kit.chat-system-prompt:你是内部助手，回答简洁、准确，使用中文。}") String chatPrompt,
            @Value("${ruoyi.ai-kit.risk-system-prompt:你是风控分析助手。根据用户描述评估风险等级（低/中/高）并给出理由，使用中文。}") String riskPrompt,
            @Value("${ruoyi.ai-kit.rag-system-prompt:你是知识问答助手。基于已知政策与常识回答，不确定时明确说明，使用中文。}") String ragPrompt,
            @Value("${ruoyi.ai-kit.ops-system-prompt:你是编排中台运维助手。根据用户问题给出可操作的排查建议，不要编造不存在的链路，使用中文。}") String opsPrompt)
    {
        this.prompts = Map.of(
                "chat", chatPrompt,
                "risk", riskPrompt,
                "rag", ragPrompt,
                "ops", opsPrompt);
    }

    @Override
    public AgentDefinition load(String agentCode)
    {
        if (StringUtils.isEmpty(agentCode))
        {
            return null;
        }
        String code = agentCode.trim().toLowerCase();
        String prompt = prompts.get(code);
        if (prompt == null)
        {
            return null;
        }
        AgentDefinition def = new AgentDefinition();
        def.setAgentCode(code);
        def.setAgentName(code);
        def.setSystemPrompt(prompt);
        def.setTemperature(0.3);
        def.setEnabled(true);
        return def;
    }
}
