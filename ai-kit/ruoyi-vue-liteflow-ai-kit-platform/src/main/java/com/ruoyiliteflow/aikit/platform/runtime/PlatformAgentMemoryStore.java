package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.spi.AgentMemoryStore;
import com.ruoyiliteflow.aicore.spi.MemoryItem;
import com.ruoyiliteflow.aikit.platform.domain.AiMemoryItem;
import com.ruoyiliteflow.aikit.platform.mapper.AiMemoryItemMapper;
import com.ruoyiliteflow.common.utils.StringUtils;

@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformAgentMemoryStore implements AgentMemoryStore
{
    @Autowired
    private AiMemoryItemMapper aiMemoryItemMapper;

    @Override
    public List<MemoryItem> loadRecent(String agentCode, String sessionId, String principal, int limit)
    {
        if (StringUtils.isEmpty(agentCode) || limit <= 0)
        {
            return Collections.emptyList();
        }
        String sid = StringUtils.isEmpty(sessionId) ? "default" : sessionId;
        List<AiMemoryItem> rows = aiMemoryItemMapper.selectRecent(agentCode, sid, limit);
        if (rows == null || rows.isEmpty())
        {
            return Collections.emptyList();
        }
        List<MemoryItem> items = new ArrayList<>(rows.size());
        for (AiMemoryItem row : rows)
        {
            items.add(toSpi(row));
        }
        return items;
    }

    @Override
    public void save(MemoryItem item)
    {
        if (item == null || StringUtils.isEmpty(item.getAgentCode()) || StringUtils.isEmpty(item.getContent()))
        {
            return;
        }
        AiMemoryItem row = new AiMemoryItem();
        row.setAgentCode(item.getAgentCode());
        row.setSessionId(StringUtils.isEmpty(item.getSessionId()) ? "default" : item.getSessionId());
        row.setPrincipal(StringUtils.isEmpty(item.getPrincipal()) ? "anonymous" : item.getPrincipal());
        row.setMemoryType(StringUtils.isEmpty(item.getMemoryType()) ? "turn" : item.getMemoryType());
        row.setRole(item.getRole());
        row.setContent(item.getContent());
        row.setCreateBy("runtime");
        aiMemoryItemMapper.insertAiMemoryItem(row);
    }

    @Override
    public int countTurns(String agentCode, String sessionId)
    {
        if (StringUtils.isEmpty(agentCode))
        {
            return 0;
        }
        String sid = StringUtils.isEmpty(sessionId) ? "default" : sessionId;
        return aiMemoryItemMapper.countTurns(agentCode, sid);
    }

    @Override
    public int countByType(String agentCode, String sessionId, String memoryType)
    {
        if (StringUtils.isEmpty(agentCode) || StringUtils.isEmpty(memoryType))
        {
            return 0;
        }
        String sid = StringUtils.isEmpty(sessionId) ? "default" : sessionId;
        return aiMemoryItemMapper.countByType(agentCode, sid, memoryType);
    }

    private static MemoryItem toSpi(AiMemoryItem row)
    {
        MemoryItem item = new MemoryItem();
        item.setAgentCode(row.getAgentCode());
        item.setSessionId(row.getSessionId());
        item.setPrincipal(row.getPrincipal());
        item.setMemoryType(row.getMemoryType());
        item.setRole(row.getRole());
        item.setContent(row.getContent());
        return item;
    }
}
