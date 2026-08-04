package com.ruoyiliteflow.aicore.runtime;

/**
 * 按 agentCode 加载运行时定义。platform 提供 DB 实现；无 DB 时可提供 yml/内存实现。
 */
public interface AgentDefinitionProvider
{
    AgentDefinition load(String agentCode);
}
