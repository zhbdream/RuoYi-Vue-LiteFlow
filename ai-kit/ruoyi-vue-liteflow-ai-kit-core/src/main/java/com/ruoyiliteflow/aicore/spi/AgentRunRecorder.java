package com.ruoyiliteflow.aicore.spi;

/** 可选：记录每次 Agent 调用。platform 落库，core 不依赖表结构。 */
public interface AgentRunRecorder
{
    default void record(AgentRunLog log)
    {
    }
}
