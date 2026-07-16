package com.ruoyiliteflow.langchain.service;

import java.util.List;
import java.util.function.Consumer;
import com.ruoyiliteflow.langchain.domain.LfChatMessage;
import com.ruoyiliteflow.langchain.domain.LfChatSession;
import com.ruoyiliteflow.langchain.domain.vo.LfChatStreamEventVo;

/**
 * 内部 AI 助手：会话管理 + 流式对话
 */
public interface ILfChatService
{
    List<LfChatSession> selectSessionList(String username);

    LfChatSession createSession(String username, String title);

    List<LfChatMessage> selectMessages(Long sessionId, String username);

    int deleteSessions(Long[] ids, String username);

    /**
     * 流式发送。delta 事件走 onDelta；完成时返回 done 载荷。
     */
    LfChatStreamEventVo streamChat(Long sessionId, String content, String username,
            Consumer<LfChatStreamEventVo> onDelta);
}
