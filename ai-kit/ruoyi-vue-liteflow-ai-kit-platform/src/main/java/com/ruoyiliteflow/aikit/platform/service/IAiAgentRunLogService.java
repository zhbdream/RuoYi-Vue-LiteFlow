package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiAgentRunLog;

public interface IAiAgentRunLogService
{
    List<AiAgentRunLog> selectAiAgentRunLogList(AiAgentRunLog query);
}
