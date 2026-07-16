package com.ruoyiliteflow.langchain.mapper;

import java.util.List;
import com.ruoyiliteflow.langchain.domain.LfChatSession;

public interface LfChatSessionMapper
{
    List<LfChatSession> selectLfChatSessionList(LfChatSession query);

    LfChatSession selectLfChatSessionById(Long id);

    int insertLfChatSession(LfChatSession session);

    int updateLfChatSession(LfChatSession session);

    int deleteLfChatSessionByIds(Long[] ids);
}
