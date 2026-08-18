package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiMemoryItem;

public interface AiMemoryItemMapper
{
    List<AiMemoryItem> selectAiMemoryItemList(AiMemoryItem query);

    List<AiMemoryItem> selectRecent(@Param("agentCode") String agentCode,
            @Param("sessionId") String sessionId, @Param("limit") int limit);

    int countTurns(@Param("agentCode") String agentCode, @Param("sessionId") String sessionId);

    int countByType(@Param("agentCode") String agentCode, @Param("sessionId") String sessionId,
            @Param("memoryType") String memoryType);

    int insertAiMemoryItem(AiMemoryItem item);

    int deleteAiMemoryItemByIds(Long[] ids);

    int deleteByAgentSession(@Param("agentCode") String agentCode, @Param("sessionId") String sessionId);

    int deleteExpired(@Param("days") int days);
}
