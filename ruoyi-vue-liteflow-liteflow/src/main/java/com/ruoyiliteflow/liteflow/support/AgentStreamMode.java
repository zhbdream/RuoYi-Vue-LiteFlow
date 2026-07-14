package com.ruoyiliteflow.liteflow.support;

/**
 * 标记当前执行是否启用 Agent 模型流式（供 ReActAgentComponent.model() 读取）
 */
public final class AgentStreamMode
{
    private static final ThreadLocal<Boolean> FLAG = new ThreadLocal<>();

    private AgentStreamMode()
    {
    }

    public static void enable()
    {
        FLAG.set(Boolean.TRUE);
    }

    public static boolean isEnabled()
    {
        return Boolean.TRUE.equals(FLAG.get());
    }

    public static void clear()
    {
        FLAG.remove();
    }
}
