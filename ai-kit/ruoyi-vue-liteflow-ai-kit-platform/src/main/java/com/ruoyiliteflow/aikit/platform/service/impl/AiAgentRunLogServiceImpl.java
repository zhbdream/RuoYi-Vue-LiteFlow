package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aikit.platform.domain.AiAgentRunLog;
import com.ruoyiliteflow.aikit.platform.mapper.AiAgentRunLogMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentRunLogService;

@Service
public class AiAgentRunLogServiceImpl implements IAiAgentRunLogService
{
    @Autowired
    private AiAgentRunLogMapper aiAgentRunLogMapper;

    @Override
    public List<AiAgentRunLog> selectAiAgentRunLogList(AiAgentRunLog query)
    {
        return aiAgentRunLogMapper.selectAiAgentRunLogList(query);
    }
}
