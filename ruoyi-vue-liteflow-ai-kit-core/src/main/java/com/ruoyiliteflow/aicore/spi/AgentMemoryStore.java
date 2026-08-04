package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;

public interface AgentMemoryStore
{
    default List<MemoryItem> loadRecent(String agentCode, String sessionId, String principal, int limit)
    {
        return Collections.emptyList();
    }

    default void save(MemoryItem item)
    {
    }

    default int countTurns(String agentCode, String sessionId)
    {
        return 0;
    }
}
