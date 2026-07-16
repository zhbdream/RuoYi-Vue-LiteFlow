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
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
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
            Consumer<LfChatStreamEventVo> onDelta)
    {
        if (StringUtils.isEmpty(content) || StringUtils.isEmpty(content.trim()))
        {
            throw new ServiceException("消息内容不能为空");
        }
        String userText = content.trim();
        agentQuotaService.assertWithinQuota(username, QUOTA_CHAIN);

        Lc4jModelCredential cred = chatModelFactory.resolveCredential();
        LfChatSession session = resolveOrCreateSession(sessionId, username, userText, cred);

        LfChatMessage userMsg = new LfChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole(ROLE_USER);
        userMsg.setContent(userText);
        userMsg.setCreateBy(username);
        messageMapper.insertLfChatMessage(userMsg);

        List<ChatMessage> messages = buildModelMessages(session.getId());
        StreamingChatModel model = chatModelFactory.createStreamingChatModel(temperature);

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

        LfChatMessage assistantMsg = new LfChatMessage();
        assistantMsg.setSessionId(session.getId());
        assistantMsg.setRole(ROLE_ASSISTANT);
        assistantMsg.setContent(answer);
        assistantMsg.setTokenCount(tokens);
        assistantMsg.setCreateBy(username);
        messageMapper.insertLfChatMessage(assistantMsg);

        session.setModelCode(cred.getModelName());
        session.setModelName(cred.getModelName());
        session.setUpdateBy(username);
        sessionMapper.updateLfChatSession(session);

        long tokenLong = tokens == null ? 0L : tokens.longValue();
        agentQuotaService.recordUsage(username, QUOTA_CHAIN, tokenLong);

        return LfChatStreamEventVo.done(session.getId(), assistantMsg.getId(), answer, cred.getModelName(),
                session.getTitle());
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
            Lc4jModelCredential cred)
    {
        if (sessionId == null)
        {
            LfChatSession created = createSession(username, firstMessage);
            created.setModelCode(cred.getModelName());
            created.setModelName(cred.getModelName());
            sessionMapper.updateLfChatSession(created);
            return created;
        }
        LfChatSession session = requireOwnedSession(sessionId, username);
        if ("新对话".equals(session.getTitle()) || StringUtils.isEmpty(session.getTitle()))
        {
            session.setTitle(trimTitle(firstMessage));
        }
        return session;
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
