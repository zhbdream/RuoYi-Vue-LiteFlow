package com.ruoyiliteflow.aicore.runtime;

/**
 * 配置驱动的智能体执行入口。
 */
public interface AgentRuntime
{
    AgentRunResult invoke(String agentCode, AgentRunRequest request);
}
