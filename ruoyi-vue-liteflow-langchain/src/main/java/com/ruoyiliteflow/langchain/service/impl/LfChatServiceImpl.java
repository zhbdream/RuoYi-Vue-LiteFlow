package com.ruoyiliteflow.langchain.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunResult;
import com.ruoyiliteflow.aicore.runtime.AgentRuntime;
import com.ruoyiliteflow.aicore.spi.AgentStreamListener;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.LfChatMessage;
import com.ruoyiliteflow.langchain.domain.LfChatSession;
import com.ruoyiliteflow.langchain.domain.vo.LfChatStreamEventVo;
import com.ruoyiliteflow.langchain.mapper.LfChatMessageMapper;
import com.ruoyiliteflow.langchain.mapper.LfChatSessionMapper;
import com.ruoyiliteflow.langchain.service.ILfChatService;
import com.ruoyiliteflow.langchain.support.Lc4jChatModelFactory;
import com.ruoyiliteflow.langchain.support.Lc4jModelCredential;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;

@Service
public class LfChatServiceImpl implements ILfChatService
{
    private static final String QUOTA_CHAIN = "aiChat";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    @Autowired
    private LfChatSessionMapper sessionMapper;

    @Autowired
    private LfChatMessageMapper messageMapper;

    @Autowired
    private Lc4jChatModelFactory chatModelFactory;

    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Autowired(required = false)
    private AgentRuntime agentRuntime;

    @Value("${liteflow.chat.history-limit:20}")
    private int historyLimit;

    @Value("${liteflow.chat.temperature:0.3}")
    private double temperature;

    @Value("${liteflow.chat.system-prompt:你是 RuoYi-Vue-LiteFlow 平台的内部 AI 助手。帮助用户理解 LiteFlow 编排、链路调试、模型配置与业务规则。回答简洁、准确，必要时给出可操作步骤。}")
    private String systemPrompt;

    @Override
    public List<LfChatSession> selectSessionList(String username)
    {
        LfChatSession query = new LfChatSession();
        query.setUserName(username);
        return sessionMapper.selectLfChatSessionList(query);
    }

    @Override
    public LfChatSession createSession(String username, String title)
    {
        LfChatSession session = new LfChatSession();
        session.setTitle(StringUtils.isNotEmpty(title) ? trimTitle(title) : "新对话");
        session.setUserName(username);
        session.setStatus("0");
        session.setCreateBy(username);
        session.setUpdateBy(username);
        sessionMapper.insertLfChatSession(session);
        return session;
    }

    @Override
    public List<LfChatMessage> selectMessages(Long sessionId, String username)
    {
        requireOwnedSession(sessionId, username);
        return messageMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional
    public int deleteSessions(Long[] ids, String username)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        for (Long id : ids)
        {
            requireOwnedSession(id, username);
        }
        messageMapper.deleteBySessionIds(ids);
        return sessionMapper.deleteLfChatSessionByIds(ids);
    }

    @Override
    public LfChatStreamEventVo streamChat(Long sessionId, String content, String username,
            String modelCode, String agentCode, Consumer<LfChatStreamEventVo> onDelta)
    {
        if (StringUtils.isEmpty(content) || StringUtils.isEmpty(content.trim()))
        {
            throw new ServiceException("消息内容不能为空");
        }
        String userText = content.trim();
        LfChatSession session = resolveOrCreateSession(sessionId, username, userText, modelCode, agentCode);
        String pinnedAgent = session.getAgentCode();
        if (StringUtils.isNotEmpty(pinnedAgent))
        {
            return streamViaAgent(session, userText, username, pinnedAgent, onDelta);
        }
        agentQuotaService.assertWithinQuota(username, QUOTA_CHAIN);
        String pinnedModel = session.getModelCode();
        Lc4jModelCredential cred = chatModelFactory.resolveCredential(pinnedModel);
        return streamViaModel(session, userText, username, cred, onDelta);
    }

    private LfChatStreamEventVo streamViaModel(LfChatSession session, String userText, String username,
            Lc4jModelCredential cred, Consumer<LfChatStreamEventVo> onDelta)
    {
        insertMessage(session.getId(), ROLE_USER, userText, username, null);

        List<ChatMessage> messages = buildModelMessages(session.getId());
        StreamingChatModel model = chatModelFactory.createStreamingChatModel(temperature, cred);

        StringBuilder full = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();

        model.chat(messages, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                if (partialResponse == null || partialResponse.isEmpty())
                {
                    return;
                }
                full.append(partialResponse);
                if (onDelta != null)
                {
                    onDelta.accept(LfChatStreamEventVo.delta(partialResponse));
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response)
            {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error)
            {
                errorRef.set(error);
                latch.countDown();
            }
        });

        awaitLatch(latch);
        if (errorRef.get() != null)
        {
            Throwable err = errorRef.get();
            throw new ServiceException(err.getMessage() == null ? "模型调用失败" : err.getMessage());
        }

        String answer = full.toString();
        if (StringUtils.isEmpty(answer) && responseRef.get() != null && responseRef.get().aiMessage() != null)
        {
            answer = responseRef.get().aiMessage().text();
        }
        if (StringUtils.isEmpty(answer))
        {
            answer = "（模型未返回内容）";
        }

        Integer tokens = null;
        ChatResponse completed = responseRef.get();
        if (completed != null)
        {
            TokenUsage usage = completed.tokenUsage();
            if (usage != null && usage.totalTokenCount() != null)
            {
                tokens = usage.totalTokenCount();
            }
        }

        LfChatMessage assistantMsg = insertMessage(session.getId(), ROLE_ASSISTANT, answer, username, tokens);
        String display = StringUtils.isNotEmpty(session.getModelName()) ? session.getModelName() : cred.getModelName();
        touchSession(session, username);
        agentQuotaService.recordUsage(username, QUOTA_CHAIN, tokens == null ? 0L : tokens.longValue());
        return LfChatStreamEventVo.done(session.getId(), assistantMsg.getId(), answer, display, session.getTitle(),
                null);
    }

    private LfChatStreamEventVo streamViaAgent(LfChatSession session, String userText, String username,
            String agentCode, Consumer<LfChatStreamEventVo> onDelta)
    {
        if (agentRuntime == null)
        {
            throw new ServiceException("智能体运行时未就绪，请确认已启用 AI Kit");
        }
        insertMessage(session.getId(), ROLE_USER, userText, username, null);

        AgentRunRequest request = new AgentRunRequest();
        request.setMessage(userText);
        request.setPrincipal(username);
        request.setSessionId("lf-chat-" + session.getId());

        StringBuilder full = new StringBuilder();
        List<Object> toolTrace = new ArrayList<>();
        AgentRunResult result = agentRuntime.stream(agentCode, request, new AgentStreamListener()
        {
            @Override
            public void onDelta(String token)
            {
                if (token == null || token.isEmpty())
                {
                    return;
                }
                full.append(token);
                if (onDelta != null)
                {
                    onDelta.accept(LfChatStreamEventVo.delta(token));
                }
            }

            @Override
            public void onTool(Object trace)
            {
                if (trace != null)
                {
                    toolTrace.add(trace);
                }
                if (onDelta != null)
                {
                    onDelta.accept(LfChatStreamEventVo.toolEvent(trace));
                }
            }
        });

        String answer = full.toString();
        if (StringUtils.isEmpty(answer) && result != null)
        {
            answer = result.getContent();
        }
        if (StringUtils.isEmpty(answer))
        {
            answer = "（智能体未返回内容）";
        }

        LfChatMessage assistantMsg = insertMessage(session.getId(), ROLE_ASSISTANT, answer, username, null);
        String display = StringUtils.isNotEmpty(session.getModelName()) ? session.getModelName()
                : (result != null && StringUtils.isNotEmpty(result.getModel()) ? result.getModel() : agentCode);
        touchSession(session, username);
        String model = result != null && StringUtils.isNotEmpty(result.getModel()) ? result.getModel() : display;
        LfChatStreamEventVo done = LfChatStreamEventVo.done(session.getId(), assistantMsg.getId(), answer, model,
                session.getTitle(), agentCode);
        if (!toolTrace.isEmpty())
        {
            done.setTools(toolTrace);
        }
        else if (result != null && result.getToolTrace() != null && !result.getToolTrace().isEmpty())
        {
            done.setTools(result.getToolTrace());
        }
        return done;
    }

    private LfChatMessage insertMessage(Long sessionId, String role, String content, String username, Integer tokens)
    {
        LfChatMessage msg = new LfChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokenCount(tokens);
        msg.setCreateBy(username);
        messageMapper.insertLfChatMessage(msg);
        return msg;
    }

    private void touchSession(LfChatSession session, String username)
    {
        session.setUpdateBy(username);
        sessionMapper.updateLfChatSession(session);
    }

    private void awaitLatch(CountDownLatch latch)
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

    private List<ChatMessage> buildModelMessages(Long sessionId)
    {
        int limit = historyLimit > 0 ? historyLimit : 20;
        List<LfChatMessage> history = messageMapper.selectRecentBySessionId(sessionId, limit);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        for (LfChatMessage msg : history)
        {
            if (ROLE_USER.equals(msg.getRole()))
            {
                messages.add(UserMessage.from(msg.getContent()));
            }
            else if (ROLE_ASSISTANT.equals(msg.getRole()))
            {
                messages.add(new AiMessage(msg.getContent()));
            }
        }
        return messages;
    }

    private LfChatSession resolveOrCreateSession(Long sessionId, String username, String firstMessage,
            String modelCode, String agentCode)
    {
        if (sessionId == null)
        {
            LfChatSession created = createSession(username, firstMessage);
            applyPick(created, modelCode, agentCode);
            sessionMapper.updateLfChatSession(created);
            return created;
        }
        LfChatSession session = requireOwnedSession(sessionId, username);
        if ("新对话".equals(session.getTitle()) || StringUtils.isEmpty(session.getTitle()))
        {
            session.setTitle(trimTitle(firstMessage));
        }
        boolean pinned = StringUtils.isNotEmpty(session.getAgentCode()) || StringUtils.isNotEmpty(session.getModelCode());
        if (!pinned)
        {
            applyPick(session, modelCode, agentCode);
        }
        return session;
    }

    private void applyPick(LfChatSession session, String modelCode, String agentCode)
    {
        if (StringUtils.isNotEmpty(agentCode))
        {
            session.setAgentCode(agentCode.trim());
            session.setModelName("智能体 · " + agentCode.trim());
            session.setModelCode("");
            return;
        }
        session.setAgentCode("");
        if (StringUtils.isNotEmpty(modelCode))
        {
            session.setModelCode(modelCode.trim());
            LfAgentModel model = lfAgentModelService.resolveRuntimeByCode(modelCode.trim());
            String name = model == null ? modelCode.trim()
                    : (StringUtils.isNotEmpty(model.getModelName()) ? model.getModelName() : model.getModel());
            session.setModelName(name);
        }
        else
        {
            LfAgentModel def = lfAgentModelService.resolveRuntimeDefault();
            if (def != null)
            {
                session.setModelCode(def.getModelCode());
                session.setModelName(StringUtils.isNotEmpty(def.getModelName()) ? def.getModelName() : def.getModel());
            }
        }
    }

    private LfChatSession requireOwnedSession(Long sessionId, String username)
    {
        LfChatSession session = sessionMapper.selectLfChatSessionById(sessionId);
        if (session == null)
        {
            throw new ServiceException("会话不存在或已删除");
        }
        if (!username.equals(session.getUserName()))
        {
            throw new ServiceException("无权访问该会话");
        }
        return session;
    }

    private static String trimTitle(String text)
    {
        String t = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (t.length() > 40)
        {
            return t.substring(0, 40) + "...";
        }
        return StringUtils.isEmpty(t) ? "新对话" : t;
    }
}
