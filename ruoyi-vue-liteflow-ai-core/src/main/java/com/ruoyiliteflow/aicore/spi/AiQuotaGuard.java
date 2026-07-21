package com.ruoyiliteflow.aicore.spi;

/**
 * 配额守卫。无实现时不做限制（便于独立进程调试）。
 */
public interface AiQuotaGuard
{
    void assertWithinQuota(String principal, String dimension);
}
