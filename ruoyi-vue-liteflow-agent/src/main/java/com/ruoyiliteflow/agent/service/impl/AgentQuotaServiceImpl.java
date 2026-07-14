package com.ruoyiliteflow.agent.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.core.redis.RedisCache;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AgentQuotaServiceImpl implements IAgentQuotaService
{
    private static final Logger log = LoggerFactory.getLogger(AgentQuotaServiceImpl.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Value("${liteflow.agent.quota.enabled:true}")
    private boolean enabled;

    @Value("${liteflow.agent.quota.daily-call-limit:100}")
    private int globalDailyCallLimit;

    @Value("${liteflow.agent.quota.daily-token-limit:200000}")
    private int globalDailyTokenLimit;

    @Value("${liteflow.agent.quota.daily-chain-call-limit:200}")
    private int globalDailyChainCallLimit;

    @Override
    public void assertWithinQuota(String username, String chainName)
    {
        if (!enabled)
        {
            return;
        }
        try
        {
            String user = normalizeUser(username);
            String day = LocalDate.now().format(DAY);
            int callLimit = resolveCallLimit();
            int tokenLimit = resolveTokenLimit();

            long userCalls = getLong(key("calls", "user", user, day));
            if (userCalls >= callLimit)
            {
                throw new ServiceException("今日 Agent 调用次数已达上限（" + callLimit + "），请明日再试或调整配额");
            }
            long userTokens = getLong(key("tokens", "user", user, day));
            if (userTokens >= tokenLimit)
            {
                throw new ServiceException("今日 Agent Token 已达上限（" + tokenLimit + "），请明日再试或调整配额");
            }
            if (StringUtils.isNotEmpty(chainName))
            {
                long chainCalls = getLong(key("calls", "chain", chainName, day));
                if (chainCalls >= globalDailyChainCallLimit)
                {
                    throw new ServiceException("链路 " + chainName + " 今日 Agent 调用已达上限（" + globalDailyChainCallLimit + "）");
                }
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.warn("Agent quota check skipped: {}", e.getMessage());
        }
    }

    @Override
    public void recordUsage(String username, String chainName, long tokens)
    {
        if (!enabled)
        {
            return;
        }
        try
        {
            String user = normalizeUser(username);
            String day = LocalDate.now().format(DAY);
            incr(key("calls", "user", user, day), 1);
            incr(key("tokens", "user", user, day), Math.max(tokens, 0));
            if (StringUtils.isNotEmpty(chainName))
            {
                incr(key("calls", "chain", chainName, day), 1);
            }
        }
        catch (Exception e)
        {
            log.warn("Agent quota record skipped: {}", e.getMessage());
        }
    }

    private int resolveCallLimit()
    {
        LfAgentModel def = safeDefault();
        if (def != null && def.getDailyCallLimit() != null && def.getDailyCallLimit() > 0)
        {
            return def.getDailyCallLimit();
        }
        return globalDailyCallLimit;
    }

    private int resolveTokenLimit()
    {
        LfAgentModel def = safeDefault();
        if (def != null && def.getDailyTokenLimit() != null && def.getDailyTokenLimit() > 0)
        {
            return def.getDailyTokenLimit();
        }
        return globalDailyTokenLimit;
    }

    private LfAgentModel safeDefault()
    {
        try
        {
            return lfAgentModelService.resolveRuntimeDefault();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String normalizeUser(String username)
    {
        return StringUtils.isEmpty(username) ? "anonymous" : username;
    }

    private String key(String metric, String scope, String id, String day)
    {
        return "liteflow:agent:quota:" + metric + ":" + scope + ":" + id + ":" + day;
    }

    private long getLong(String k)
    {
        Object v = redisCache.getCacheObject(k);
        if (v == null)
        {
            return 0L;
        }
        if (v instanceof Number n)
        {
            return n.longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(v));
        }
        catch (Exception e)
        {
            return 0L;
        }
    }

    private void incr(String k, long delta)
    {
        if (delta <= 0)
        {
            return;
        }
        long cur = getLong(k);
        redisCache.setCacheObject(k, Long.valueOf(cur + delta), 2, TimeUnit.DAYS);
    }
}
