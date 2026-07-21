package com.ruoyiliteflow.langchain.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 将现有 Redis 日配额接到 ai-core Facade。
 */
@Component
public class AgentQuotaGuardAdapter implements AiQuotaGuard
{
    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Override
    public void assertWithinQuota(String principal, String dimension)
    {
        String user = StringUtils.isEmpty(principal) ? "anonymous" : principal;
        String dim = StringUtils.isEmpty(dimension) ? "ai-core" : dimension;
        agentQuotaService.assertWithinQuota(user, dim);
    }
}
