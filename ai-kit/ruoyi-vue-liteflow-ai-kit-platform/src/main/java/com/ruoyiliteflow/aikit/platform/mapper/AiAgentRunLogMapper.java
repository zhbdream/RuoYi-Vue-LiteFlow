package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiAgentRunLog;

public interface AiAgentRunLogMapper
{
    List<AiAgentRunLog> selectAiAgentRunLogList(AiAgentRunLog query);

    int insertAiAgentRunLog(AiAgentRunLog row);
}
