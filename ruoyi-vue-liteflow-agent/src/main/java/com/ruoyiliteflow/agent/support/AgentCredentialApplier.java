package com.ruoyiliteflow.agent.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;

/**
 * 将库中默认模型凭据注入到 LiteFlow AgentConfig（优先于空的 yml）
 */
@Component
public class AgentCredentialApplier
{
    private static final long CACHE_TTL_MS = 15_000L;

    @Autowired
    private ILfAgentModelService lfAgentModelService;

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    /**
     * @return 运行时选用的模型名（可覆盖组件默认）；无 DB 配置时返回 null
     */
    public String applyDefault(AgentConfig agentConfig)
    {
        if (agentConfig == null)
        {
            return null;
        }
        LfAgentModel model = resolveCached();
        if (model == null || StringUtils.isEmpty(model.getApiKey()))
        {
            return null;
        }
        String configKey = StringUtils.isEmpty(model.getConfigKey()) ? "deepseek" : model.getConfigKey();
        Map<String, PlatformCredential> map = agentConfig.getOpenaiCompatible();
        if (map == null)
        {
            map = new HashMap<>();
            agentConfig.setOpenaiCompatible(map);
        }
        PlatformCredential cred = map.computeIfAbsent(configKey, k -> new PlatformCredential());
        cred.setApiKey(model.getApiKey());
        if (StringUtils.isNotEmpty(model.getBaseUrl()))
        {
            cred.setBaseUrl(model.getBaseUrl());
        }
        return model.getModel();
    }

    /** 模型配置变更后可主动清缓存（可选） */
    public void invalidate()
    {
        cache.set(null);
    }

    private LfAgentModel resolveCached()
    {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get();
        if (entry != null && now - entry.at < CACHE_TTL_MS)
        {
            return entry.model;
        }
        LfAgentModel model = lfAgentModelService.resolveRuntimeDefault();
        cache.set(new CacheEntry(model, now));
        return model;
    }

    private static final class CacheEntry
    {
        private final LfAgentModel model;
        private final long at;

        private CacheEntry(LfAgentModel model, long at)
        {
            this.model = model;
            this.at = at;
        }
    }
}
