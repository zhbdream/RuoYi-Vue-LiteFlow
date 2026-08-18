package com.ruoyiliteflow.aicore.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.spi.AgentMemoryStore;
import com.ruoyiliteflow.aicore.spi.AgentRunLog;
import com.ruoyiliteflow.aicore.spi.AgentRunRecorder;
import com.ruoyiliteflow.aicore.spi.AgentStreamListener;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.aicore.spi.KnowledgeRetriever;
import com.ruoyiliteflow.aicore.spi.MemoryItem;
import com.ruoyiliteflow.aicore.spi.SkillResolver;
import com.ruoyiliteflow.aicore.spi.SkillSpec;
import com.ruoyiliteflow.aicore.spi.ToolCatalog;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.aicore.spi.ToolExecutor;
import com.ruoyiliteflow.aicore.support.AiChatModelFactory;
import com.ruoyiliteflow.aicore.skill.SkillRenderer;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

@Service
public class DefaultAgentRuntime implements AgentRuntime
{
    private static final Logger log = LoggerFactory.getLogger(DefaultAgentRuntime.class);
    private static final int MAX_TOOL_ROUNDS = 5;

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

    @Autowired(required = false)
    private ToolExecutor toolExecutor;

    @Autowired(required = false)
    private AgentRunRecorder runRecorder;

    @Value("${ruoyi.ai.quota.dimension.aikit:aikit}")
    private String quotaDimensionPrefix;

    @Value("${ruoyi.ai.rag.max-results:3}")
    private int ragMaxResults;

    @Value("${ruoyi.ai.rag.min-score:0.45}")
    private double ragMinScore;

    @Override
    public AgentRunResult invoke(String agentCode, AgentRunRequest request)
    {
        return stream(agentCode, request, null);
    }

    @Override
    public AgentRunResult stream(String agentCode, AgentRunRequest request, AgentStreamListener listener)
    {
        long start = System.currentTimeMillis();
        Prepared prepared = null;
        List<Object> toolTrace = new ArrayList<>();
        try
        {
            prepared = prepare(agentCode, request);
            toolTrace = prepared.skillTrace;
            String content;
            if (prepared.toolSpecs.isEmpty())
            {
                content = generate(prepared, listener);
            }
            else
            {
                content = generateWithTools(prepared, toolTrace, listener);
            }
            if (memoryStore != null)
            {
                saveTurn(memoryStore, prepared.agentCode, prepared.sessionId, prepared.principal, "user",
                        request.getMessage());
                saveTurn(memoryStore, prepared.agentCode, prepared.sessionId, prepared.principal, "assistant", content);
            }
            AgentRunResult result = new AgentRunResult(prepared.agentCode, content, prepared.cred.getModelName());
            result.setToolTrace(toolTrace.isEmpty() ? Collections.emptyList() : toolTrace);
            result.setKbHit(prepared.kbHit);
            result.setCostMs(System.currentTimeMillis() - start);
            recordRun(prepared != null ? prepared.agentCode : agentCode, prepared, request, result, null,
                    result.getCostMs());
            if (listener != null)
            {
                listener.onDone(result);
            }
            if (memoryStore != null)
            {
                final Prepared snap = prepared;
                CompletableFuture.runAsync(() -> maybeLlmSummary(snap));
            }
            log.info("AgentRuntime ok agentCode={} session={} kbHit={} tools={} costMs={}",
                    prepared.agentCode, prepared.sessionId, prepared.kbHit, toolTrace.size(),
                    result.getCostMs());
            return result;
        }
        catch (RuntimeException e)
        {
            long cost = System.currentTimeMillis() - start;
            log.warn("AgentRuntime fail agentCode={} costMs={} err={}",
                    agentCode, cost, e.getMessage());
            recordRun(prepared != null ? prepared.agentCode : agentCode, prepared, request, null, e.getMessage(), cost);
            if (listener != null)
            {
                listener.onError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
            throw e;
        }
    }

    private Prepared prepare(String agentCode, AgentRunRequest request)
    {
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
        int tokenBudget = policy != null ? policy.getTokenBudget() : 0;
        Map<String, String> tplVars = SkillRenderer.vars(principal, agentCode, sessionId, request.getMessage(),
                request.getVariables());
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotEmpty(def.getSystemPrompt()))
        {
            prompt.append(SkillRenderer.render(def.getSystemPrompt(), tplVars));
        }
        if (policy != null && StringUtils.isNotEmpty(policy.getVariableTemplate()))
        {
            prompt.append("\n\n【变量】").append(SkillRenderer.render(policy.getVariableTemplate(), tplVars));
        }

        List<Object> skillTrace = new ArrayList<>();
        if (skillResolver != null && def.getSkillCodes() != null && !def.getSkillCodes().isEmpty())
        {
            List<SkillSpec> skills = skillResolver.resolve(def.getSkillCodes());
            for (SkillSpec skill : skills)
            {
                if (skill == null || StringUtils.isEmpty(skill.getContent()))
                {
                    continue;
                }
                String rendered = SkillRenderer.render(skill.getContent(), tplVars);
                if ("http".equalsIgnoreCase(skill.getSkillType()))
                {
                    try
                    {
                        String body = SkillRenderer.invokeHttp(rendered);
                        prompt.append("\n\n【技能:").append(skill.getSkillCode()).append("】\n")
                                .append(StringUtils.isEmpty(body) ? "" : body);
                        SkillRenderer.HttpCall call = SkillRenderer.parseHttp(rendered);
                        skillTrace.add(Map.of("skill", skill.getSkillCode(), "type", "http",
                                "method", call.method, "url", call.url == null ? "" : call.url));
                    }
                    catch (Exception ex)
                    {
                        log.warn("skill http fetch fail code={} err={}", skill.getSkillCode(), ex.getMessage());
                    }
                }
                else
                {
                    prompt.append("\n\n【技能:").append(skill.getSkillCode()).append("】\n").append(rendered);
                    skillTrace.add(Map.of("skill", skill.getSkillCode(), "type", "prompt"));
                }
            }
        }

        List<ToolDescriptor> tools = def.getTools();
        if ((tools == null || tools.isEmpty()) && toolCatalog != null && def.getToolCodes() != null)
        {
            tools = toolCatalog.resolve(def.getToolCodes());
            def.setTools(tools);
        }
        List<ToolSpecification> toolSpecs = new ArrayList<>();
        Map<String, ToolDescriptor> toolByName = new LinkedHashMap<>();
        if (tools != null)
        {
            for (ToolDescriptor t : tools)
            {
                if (t == null || StringUtils.isEmpty(t.getToolCode()))
                {
                    continue;
                }
                String name = sanitizeToolName(t.getToolCode());
                toolByName.put(name, t);
                String desc = StringUtils.isEmpty(t.getDescription()) ? t.getToolName() : t.getDescription();
                toolSpecs.add(ToolSpecification.builder()
                        .name(name)
                        .description(StringUtils.isEmpty(desc) ? name : desc)
                        .parameters(JsonObjectSchema.builder().build())
                        .build());
            }
        }

        if (memoryStore != null)
        {
            int load = tokenBudget > 0 ? Math.max(window, 40) : window;
            List<MemoryItem> mem = memoryStore.loadRecent(agentCode, sessionId, principal, load);
            if (tokenBudget > 0)
            {
                mem = ContextWindow.trim(mem, tokenBudget);
            }
            else if (mem != null && mem.size() > window)
            {
                mem = mem.subList(Math.max(0, mem.size() - window), mem.size());
            }
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

        boolean kbHit = false;
        if (knowledgeRetriever != null && def.getKnowledgeCodes() != null && !def.getKnowledgeCodes().isEmpty())
        {
            String kbContext = knowledgeRetriever.retrieveContext(
                    def.getKnowledgeCodes(), request.getMessage(), ragMaxResults, ragMinScore);
            if (StringUtils.isNotEmpty(kbContext))
            {
                kbHit = true;
                prompt.append("\n\n【参考资料】\n").append(kbContext);
                prompt.append("\n\n请优先依据参考资料回答；资料不足时明确说明。");
            }
        }

        Prepared p = new Prepared();
        p.agentCode = agentCode;
        p.principal = principal;
        p.sessionId = sessionId;
        p.cred = cred;
        p.temperature = def.getTemperature() != null ? def.getTemperature() : 0.3;
        p.systemPrompt = prompt.toString();
        p.userMessage = request.getMessage();
        p.toolSpecs = toolSpecs;
        p.toolByName = toolByName;
        p.skillTrace = skillTrace;
        p.kbHit = kbHit;
        p.policy = policy;
        p.window = window;
        return p;
    }

    private String generate(Prepared prepared, AgentStreamListener listener)
    {
        if (listener == null)
        {
            ChatModel model = chatModelFactory.createChatModel(prepared.cred, prepared.temperature);
            List<ChatMessage> messages = List.of(SystemMessage.from(prepared.systemPrompt),
                    UserMessage.from(prepared.userMessage));
            ChatResponse resp = model.chat(ChatRequest.builder().messages(messages).build());
            return textOf(resp);
        }
        StreamingChatModel model = chatModelFactory.createStreamingChatModel(prepared.cred, prepared.temperature);
        List<ChatMessage> messages = List.of(SystemMessage.from(prepared.systemPrompt),
                UserMessage.from(prepared.userMessage));
        StringBuilder full = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        model.chat(messages, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                if (partialResponse == null || partialResponse.isEmpty() || listener.isCancelled())
                {
                    return;
                }
                full.append(partialResponse);
                listener.onDelta(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response)
            {
                latch.countDown();
            }

            @Override
            public void onError(Throwable error)
            {
                errorRef.set(error);
                latch.countDown();
            }
        });
        await(latch);
        if (errorRef.get() != null)
        {
            throw wrap(errorRef.get());
        }
        return full.toString();
    }

    private String generateWithTools(Prepared prepared, List<Object> toolTrace, AgentStreamListener listener)
    {
        ChatModel model = chatModelFactory.createChatModel(prepared.cred, prepared.temperature);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(prepared.systemPrompt
                + "\n\n需要时请调用提供的工具；工具结果返回后再给出最终中文回答。"
                + "工具失败时只说明失败原因，不要输出连接或端口排查步骤。"));
        messages.add(UserMessage.from(prepared.userMessage));

        String lastText = "";
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++)
        {
            if (listener != null && listener.isCancelled())
            {
                break;
            }
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(prepared.toolSpecs)
                    .build();
            ChatResponse response = model.chat(chatRequest);
            AiMessage ai = response.aiMessage();
            messages.add(ai);
            if (ai == null)
            {
                break;
            }
            if (ai.hasToolExecutionRequests() && !ai.toolExecutionRequests().isEmpty())
            {
                for (ToolExecutionRequest ter : ai.toolExecutionRequests())
                {
                    long t0 = System.currentTimeMillis();
                    ToolDescriptor desc = prepared.toolByName.get(ter.name());
                    String args = ter.arguments() == null ? "{}" : ter.arguments();
                    String resultText;
                    try
                    {
                        resultText = executeTool(desc, args);
                    }
                    catch (Exception ex)
                    {
                        resultText = "工具执行失败: " + ex.getMessage();
                    }
                    Map<String, Object> trace = new LinkedHashMap<>();
                    trace.put("tool", ter.name());
                    trace.put("ok", toolSucceeded(resultText));
                    trace.put("args", abbreviate(args, 400));
                    trace.put("result", abbreviate(resultText, 400));
                    trace.put("costMs", System.currentTimeMillis() - t0);
                    toolTrace.add(trace);
                    if (listener != null)
                    {
                        listener.onTool(trace);
                    }
                    messages.add(ToolExecutionResultMessage.from(ter, resultText));
                }
                continue;
            }
            lastText = ai.text() == null ? "" : ai.text();
            break;
        }
        if (StringUtils.isEmpty(lastText))
        {
            lastText = "工具已执行，但模型未给出最终回复。";
        }
        if (listener != null && !listener.isCancelled())
        {
            listener.onDelta(lastText);
        }
        return lastText;
    }

    private String executeTool(ToolDescriptor desc, String argumentsJson)
    {
        if (desc == null)
        {
            return "{\"ok\":false,\"error\":\"unknown tool\"}";
        }
        if (toolExecutor != null)
        {
            return toolExecutor.execute(desc, argumentsJson);
        }
        return "{\"ok\":true,\"tool\":\"" + desc.getToolCode() + "\",\"echo\":" + safeJson(argumentsJson) + "}";
    }

    private void maybeLlmSummary(Prepared prepared)
    {
        if (memoryStore == null || prepared == null || prepared.policy == null || !prepared.policy.isEnableSummary())
        {
            return;
        }
        try
        {
            int window = Math.max(prepared.window, 1);
            int turns = memoryStore.countTurns(prepared.agentCode, prepared.sessionId);
            if (turns <= window)
            {
                return;
            }
            int summaries = memoryStore.countByType(prepared.agentCode, prepared.sessionId, "summary");
            int expected = 1 + Math.max(0, turns - window) / window;
            if (summaries >= expected)
            {
                return;
            }
            List<MemoryItem> mem = memoryStore.loadRecent(prepared.agentCode, prepared.sessionId,
                    prepared.principal, Math.min(40, window * 3));
            if (mem == null || mem.isEmpty())
            {
                return;
            }
            StringBuilder conv = new StringBuilder();
            for (MemoryItem m : mem)
            {
                String role = StringUtils.isEmpty(m.getRole()) ? m.getMemoryType() : m.getRole();
                conv.append(role).append(": ").append(m.getContent() == null ? "" : m.getContent()).append("\n");
            }
            ChatModel model = chatModelFactory.createChatModel(prepared.cred, 0.2);
            String prompt = "请将以下对话压缩成不超过 200 字的中文摘要，保留关键事实、用户意图与已达成结论。只输出摘要正文，不要前言。\n\n"
                    + conv;
            ChatResponse resp = model.chat(ChatRequest.builder()
                    .messages(List.of(UserMessage.from(prompt)))
                    .build());
            String text = textOf(resp);
            if (StringUtils.isEmpty(text))
            {
                return;
            }
            MemoryItem summary = new MemoryItem();
            summary.setAgentCode(prepared.agentCode);
            summary.setSessionId(prepared.sessionId);
            summary.setPrincipal(prepared.principal);
            summary.setMemoryType("summary");
            summary.setRole("system");
            summary.setContent(text.trim());
            memoryStore.save(summary);
            log.info("AgentRuntime wrote LLM summary agentCode={} session={} turns={}",
                    prepared.agentCode, prepared.sessionId, turns);
        }
        catch (Exception e)
        {
            log.warn("AgentRuntime summary failed agentCode={} err={}", prepared.agentCode, e.getMessage());
        }
    }

    private void recordRun(String agentCode, Prepared prepared, AgentRunRequest request, AgentRunResult result,
            String error, long costMs)
    {
        if (runRecorder == null)
        {
            return;
        }
        try
        {
            AgentRunLog logRow = new AgentRunLog();
            logRow.setAgentCode(agentCode);
            if (prepared != null)
            {
                logRow.setSessionId(prepared.sessionId);
                logRow.setPrincipal(prepared.principal);
                logRow.setKbHit(prepared.kbHit);
                if (prepared.cred != null)
                {
                    logRow.setModel(prepared.cred.getModelName());
                }
                logRow.setToolTrace(prepared.skillTrace);
            }
            else if (request != null)
            {
                logRow.setSessionId(request.getSessionId());
                logRow.setPrincipal(request.getPrincipal());
            }
            if (result != null)
            {
                if (StringUtils.isNotEmpty(result.getModel()))
                {
                    logRow.setModel(result.getModel());
                }
                logRow.setKbHit(result.isKbHit());
                logRow.setToolTrace(result.getToolTrace());
            }
            logRow.setCostMs(costMs);
            logRow.setErrorMsg(error);
            if (request != null)
            {
                logRow.setUserMessage(abbreviate(request.getMessage(), 500));
            }
            runRecorder.record(logRow);
        }
        catch (Exception e)
        {
            log.warn("AgentRuntime record failed: {}", e.getMessage());
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

    private static String textOf(ChatResponse resp)
    {
        if (resp == null || resp.aiMessage() == null || resp.aiMessage().text() == null)
        {
            return "";
        }
        return resp.aiMessage().text();
    }

    private static void await(CountDownLatch latch)
    {
        try
        {
            if (!latch.await(170, TimeUnit.SECONDS))
            {
                throw new ServiceException("模型响应超时，请稍后重试");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("对话被中断");
        }
    }

    private static RuntimeException wrap(Throwable t)
    {
        if (t instanceof RuntimeException re)
        {
            return re;
        }
        return new ServiceException(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
    }

    private static String sanitizeToolName(String code)
    {
        return code.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static boolean toolSucceeded(String resultText)
    {
        if (resultText == null || resultText.isEmpty())
        {
            return false;
        }
        String t = resultText;
        if (t.contains("工具执行失败") || t.contains("Connection refused") || t.contains("ConnectException"))
        {
            return false;
        }
        String compact = t.replace(" ", "");
        return !compact.contains("\"ok\":false");
    }

    private static String abbreviate(String s, int max)
    {
        if (s == null)
        {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String safeJson(String argumentsJson)
    {
        if (StringUtils.isEmpty(argumentsJson))
        {
            return "{}";
        }
        String t = argumentsJson.trim();
        if (t.startsWith("{") || t.startsWith("["))
        {
            return t;
        }
        return "\"" + t.replace("\"", "\\\"") + "\"";
    }

    private static final class Prepared
    {
        String agentCode;
        String principal;
        String sessionId;
        AiModelCredential cred;
        double temperature;
        String systemPrompt;
        String userMessage;
        List<ToolSpecification> toolSpecs;
        Map<String, ToolDescriptor> toolByName;
        List<Object> skillTrace;
        boolean kbHit;
        AgentContextPolicy policy;
        int window;
    }
}
