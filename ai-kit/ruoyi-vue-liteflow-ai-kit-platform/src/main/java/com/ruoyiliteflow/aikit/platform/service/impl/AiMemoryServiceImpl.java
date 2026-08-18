package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aikit.platform.domain.AiMemoryItem;
import com.ruoyiliteflow.aikit.platform.mapper.AiMemoryItemMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiMemoryService;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AiMemoryServiceImpl implements IAiMemoryService
{
    @Autowired
    private AiMemoryItemMapper aiMemoryItemMapper;

    @Override
    public List<AiMemoryItem> selectAiMemoryItemList(AiMemoryItem query)
    {
        return aiMemoryItemMapper.selectAiMemoryItemList(query);
    }

    @Override
    public int insertAiMemoryItem(AiMemoryItem item)
    {
        normalize(item);
        return aiMemoryItemMapper.insertAiMemoryItem(item);
    }

    @Override
    public int deleteAiMemoryItemByIds(Long[] ids)
    {
        return aiMemoryItemMapper.deleteAiMemoryItemByIds(ids);
    }

    @Override
    public int deleteByAgentSession(String agentCode, String sessionId)
    {
        if (StringUtils.isEmpty(agentCode) || StringUtils.isEmpty(sessionId))
        {
            return 0;
        }
        return aiMemoryItemMapper.deleteByAgentSession(agentCode, sessionId);
    }

    @Override
    public int deleteExpired(int days)
    {
        if (days <= 0)
        {
            return 0;
        }
        return aiMemoryItemMapper.deleteExpired(days);
    }

    private void normalize(AiMemoryItem item)
    {
        if (StringUtils.isEmpty(item.getSessionId()))
        {
            item.setSessionId("default");
        }
        if (StringUtils.isEmpty(item.getPrincipal()))
        {
            item.setPrincipal("anonymous");
        }
        if (StringUtils.isEmpty(item.getMemoryType()))
        {
            item.setMemoryType("turn");
        }
    }
}
