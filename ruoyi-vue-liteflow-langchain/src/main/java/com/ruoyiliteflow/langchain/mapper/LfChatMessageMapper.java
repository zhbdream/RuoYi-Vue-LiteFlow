package com.ruoyiliteflow.langchain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.langchain.domain.LfChatMessage;

public interface LfChatMessageMapper
{
    List<LfChatMessage> selectBySessionId(Long sessionId);

    /** 最近 N 条（按 id 升序返回） */
    List<LfChatMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    int insertLfChatMessage(LfChatMessage message);

    int deleteBySessionIds(Long[] sessionIds);
}
