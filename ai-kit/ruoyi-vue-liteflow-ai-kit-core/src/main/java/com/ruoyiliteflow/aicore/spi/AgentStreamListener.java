package com.ruoyiliteflow.aicore.spi;

import com.ruoyiliteflow.aicore.runtime.AgentRunResult;

/**
 * Agent 流式回调（core 不绑定 SSE 协议）。
 */
public interface AgentStreamListener
{
    default void onDelta(String token)
    {
    }

    default void onTool(Object trace)
    {
    }

    default void onDone(AgentRunResult result)
    {
    }

    default void onError(String message)
    {
    }

    default boolean isCancelled()
    {
        return false;
    }
}
