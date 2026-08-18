package com.ruoyiliteflow.aicore.runtime;

import com.ruoyiliteflow.aicore.spi.AgentStreamListener;

/**
 * 配置驱动的智能体执行入口。
 */
public interface AgentRuntime
{
    AgentRunResult invoke(String agentCode, AgentRunRequest request);

    /**
     * 流式执行。无 listener 时与 {@link #invoke} 等价。
     */
    default AgentRunResult stream(String agentCode, AgentRunRequest request, AgentStreamListener listener)
    {
        AgentRunResult result = invoke(agentCode, request);
        if (listener != null)
        {
            if (result != null && result.getContent() != null)
            {
                listener.onDelta(result.getContent());
            }
            listener.onDone(result);
        }
        return result;
    }
}
