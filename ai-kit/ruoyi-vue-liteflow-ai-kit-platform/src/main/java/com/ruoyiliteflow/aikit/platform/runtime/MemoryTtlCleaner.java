package com.ruoyiliteflow.aikit.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aikit.platform.mapper.AiMemoryItemMapper;

@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class MemoryTtlCleaner
{
    private static final Logger log = LoggerFactory.getLogger(MemoryTtlCleaner.class);

    @Autowired
    private AiMemoryItemMapper memoryItemMapper;

    @Value("${ruoyi.ai-kit.memory.ttl-days:0}")
    private int ttlDays;

    @Scheduled(cron = "${ruoyi.ai-kit.memory.purge-cron:0 30 3 * * ?}")
    public void scheduledPurge()
    {
        if (ttlDays <= 0)
        {
            return;
        }
        try
        {
            int n = memoryItemMapper.deleteExpired(ttlDays);
            if (n > 0)
            {
                log.info("purged {} ai_memory_item rows older than {} days", n, ttlDays);
            }
        }
        catch (Exception e)
        {
            log.warn("memory ttl purge skipped: {}", e.getMessage());
        }
    }
}
