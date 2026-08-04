package com.ruoyiliteflow.aicore.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.spi.AgentMemoryStore;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.aicore.spi.KnowledgeRetriever;
import com.ruoyiliteflow.aicore.spi.MemoryItem;
import com.ruoyiliteflow.aicore.spi.SkillResolver;
import com.ruoyiliteflow.aicore.spi.SkillSpec;
import com.ruoyiliteflow.aicore.spi.ToolCatalog;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.aicore.support.AiChatModelFactory;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.http.HttpUtils;
import dev.langchain4j.model.chat.ChatModel;

@Service
public class DefaultAgentRuntime implements AgentRuntime
{
    private static final Logger log = LoggerFactory.getLogger(DefaultAgentRuntime.class);

    @Autowired
    private AgentDefinitionProvider definitionProvider;

    @Autowired
    private AiChatModelFactory chatModelFactory;

    @Autowired(required = false)
    private AiQuotaGuard quotaGuard;

    @Autowired(required = false)
    private KnowledgeRetriever knowledgeRetriever;

    @Autowired(required = false)
    private SkillResolver skillResolver;

    @Autowired(required = false)
    private AgentMemoryStore memoryStore;

    @Autowired(required = false)
    private ToolCatalog toolCatalog;

    @Value("${ruoyi.ai.quota.dimension.aikit:aikit}")
    private String quotaDimensionPrefix;

    @Value("${ruoyi.ai.rag.max-results:3}")
    private int ragMaxResults;

    @Value("${ruoyi.ai.rag.min-score:0.45}")
    private double ragMinScore;

    @Override
    public AgentRunResult invoke(String agentCode, AgentRunRequest request)
    {
        long start = System.currentTimeMillis();
        if (StringUtils.isEmpty(agentCode))
        {
            throw new ServiceException("agentCode 不能为空");
        }
        if (request == null || StringUtils.isEmpty(request.getMessage()))
        {
            throw new ServiceException("message 不能为空");
        }

        AgentDefinition def = definitionProvider.load(agentCode);
        if (def == null)
        {
            throw new ServiceException("智能体不存在: " + agentCode);
        }
        if (!def.isEnabled())
        {
            throw new ServiceException("智能体已停用: " + agentCode);
        }

        String principal = StringUtils.isEmpty(request.getPrincipal()) ? "anonymous" : request.getPrincipal();
        String sessionId = StringUtils.isEmpty(request.getSessionId()) ? "default" : request.getSessionId();
        if (quotaGuard != null)
        {
            quotaGuard.assertWithinQuota(principal, quotaDimensionPrefix + ":" + agentCode);
        }

        AiModelCredential cred = def.getCredential();
        if (cred == null || StringUtils.isEmpty(cred.getApiKey()))
        {
            cred = chatModelFactory.resolveCredential();
        }
        if (cred == null || StringUtils.isEmpty(cred.getApiKey()))
        {
            throw new ServiceException("未配置可用的模型 API Key（ai_model 或 ruoyi.ai.openai.api-key）");
        }

        AgentContextPolicy policy = def.getContextPolicy();
        int window = policy != null && policy.getWindowSize() > 0 ? policy.getWindowSize() : 10;

        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotEmpty(def.getSystemPrompt()))
        {
            prompt.append(def.getSystemPrompt());
        }
        if (policy != null && StringUtils.isNotEmpty(policy.getVariableTemplate()))
        {
            String vars = policy.getVariableTemplate()
                    .replace("{{principal}}", principal)
                    .replace("{{agentCode}}", agentCode)
                    .replace("{{sessionId}}", sessionId);
            prompt.append("\n\n【变量】").append(vars);
        }

        // Skills
        List<Object> toolTrace = new ArrayList<>();
        if (skillResolver != null && def.getSkillCodes() != null && !def.getSkillCodes().isEmpty())
        {
            List<SkillSpec> skills = skillResolver.resolve(def.getSkillCodes());
            for (SkillSpec skill : skills)
            {
                if (skill == null || StringUtils.isEmpty(skill.getContent()))
                {
                    continue;
                }
                if ("http".equalsIgnoreCase(skill.getSkillType()))
                {
                    try
                    {
                        String body = HttpUtils.sendGet(skill.getContent());
                        prompt.append("\n\n【技能:").append(skill.getSkillCode()).append("】\n")
                                .append(StringUtils.isEmpty(body) ? "" : body);
                        toolTrace.add(MapLite.of("skill", skill.getSkillCode(), "type", "http"));
                    }
                    catch (Exception ex)
                    {
                        log.warn("skill http fetch fail code={} err={}", skill.getSkillCode(), ex.getMessage());
                    }
                }
                else
                {
                    prompt.append("\n\n【技能:").append(skill.getSkillCode()).append("】\n").append(skill.getContent());
                    toolTrace.add(MapLite.of("skill", skill.getSkillCode(), "type", "prompt"));
                }
            }
        }

        // Tools metadata (prompt-side awareness; MCP 动态注册另走 MCP Server)
        List<ToolDescriptor> tools = def.getTools();
        if ((tools == null || tools.isEmpty()) && toolCatalog != null && def.getToolCodes() != null)
        {
            tools = toolCatalog.resolve(def.getToolCodes());
            def.setTools(tools);
        }
        if (tools != null && !tools.isEmpty())
        {
            prompt.append("\n\n【可用工具】\n");
            for (ToolDescriptor t : tools)
            {
                prompt.append("- ").append(t.getToolCode()).append(": ")
                        .append(StringUtils.isEmpty(t.getDescription()) ? t.getToolName() : t.getDescription())
                        .append("\n");
            }
        }

        // Memory
        if (memoryStore != null)
        {
            List<MemoryItem> mem = memoryStore.loadRecent(agentCode, sessionId, principal, window);
            if (mem != null && !mem.isEmpty())
            {
                prompt.append("\n\n【历史记忆】\n");
                for (MemoryItem m : mem)
                {
                    String role = StringUtils.isEmpty(m.getRole()) ? m.getMemoryType() : m.getRole();
                    prompt.append(role).append(": ").append(m.getContent()).append("\n");
                }
            }
        }

        // KB
        String kbContext = "";
        if (knowledgeRetriever != null && def.getKnowledgeCodes() != null && !def.getKnowledgeCodes().isEmpty())
        {
            kbContext = knowledgeRetriever.retrieveContext(
                    def.getKnowledgeCodes(), request.getMessage(), ragMaxResults, ragMinScore);
        }
        if (StringUtils.isNotEmpty(kbContext))
        {
            prompt.append("\n\n【参考资料】\n").append(kbContext);
            prompt.append("\n\n请优先依据参考资料回答；资料不足时明确说明。");
        }

        prompt.append("\n\n用户：").append(request.getMessage());

        double temperature = def.getTemperature() != null ? def.getTemperature() : 0.3;
        ChatModel model = chatModelFactory.createChatModel(cred, temperature);

        try
        {
            String content = model.chat(prompt.toString());
            if (memoryStore != null)
            {
                saveTurn(memoryStore, agentCode, sessionId, principal, "user", request.getMessage());
                saveTurn(memoryStore, agentCode, sessionId, principal, "assistant", content);
                if (policy != null && policy.isEnableSummary())
                {
                    int turns = memoryStore.countTurns(agentCode, sessionId);
                    if (turns > window * 2)
                    {
                        MemoryItem summary = new MemoryItem();
                        summary.setAgentCode(agentCode);
                        summary.setSessionId(sessionId);
                        summary.setPrincipal(principal);
                        summary.setMemoryType("summary");
                        summary.setRole("system");
                        summary.setContent("会话摘要节点：近期已交互约 " + turns + " 条，请优先参考最近记忆。");
                        memoryStore.save(summary);
                    }
                }
            }
            AgentRunResult result = new AgentRunResult(agentCode, content, cred.getModelName());
            result.setToolTrace(toolTrace.isEmpty() ? Collections.emptyList() : toolTrace);
            log.info("AgentRuntime invoke ok agentCode={} principal={} session={} costMs={}",
                    agentCode, principal, sessionId, System.currentTimeMillis() - start);
            return result;
        }
        catch (RuntimeException e)
        {
            log.warn("AgentRuntime invoke fail agentCode={} principal={} costMs={} err={}",
                    agentCode, principal, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    private static void saveTurn(AgentMemoryStore store, String agentCode, String sessionId, String principal,
            String role, String content)
    {
        MemoryItem item = new MemoryItem();
        item.setAgentCode(agentCode);
        item.setSessionId(sessionId);
        item.setPrincipal(principal);
        item.setMemoryType("turn");
        item.setRole(role);
        item.setContent(content);
        store.save(item);
    }

    /** 避免额外依赖，轻量 map */
    private static final class MapLite
    {
        static java.util.Map<String, Object> of(Object... kv)
        {
            java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
            for (int i = 0; i + 1 < kv.length; i += 2)
            {
                m.put(String.valueOf(kv[i]), kv[i + 1]);
            }
            return m;
        }
    }
}
