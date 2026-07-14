package com.ruoyiliteflow.agent.service;

/**
 * Agent 日调用 / Token 简易配额
 */
public interface IAgentQuotaService
{
    /** 执行前校验（含 Agent 链路时调用） */
    void assertWithinQuota(String username, String chainName);

    /** 执行后记账：calls +1，tokens 累加 */
    void recordUsage(String username, String chainName, long tokens);
}
