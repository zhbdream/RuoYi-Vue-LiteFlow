package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunResult;

public interface IAiAgentService
{
    List<AiAgent> selectAiAgentList(AiAgent query);

    AiAgent selectAiAgentById(Long id);

    AiAgent selectAiAgentByCode(String agentCode);

    int insertAiAgent(AiAgent agent);

    int updateAiAgent(AiAgent agent);

    int deleteAiAgentByIds(Long[] ids);

    AgentRunResult run(String agentCode, AgentRunRequest request);

    AgentRunResult stream(String agentCode, AgentRunRequest request, com.ruoyiliteflow.aicore.spi.AgentStreamListener listener);
}
