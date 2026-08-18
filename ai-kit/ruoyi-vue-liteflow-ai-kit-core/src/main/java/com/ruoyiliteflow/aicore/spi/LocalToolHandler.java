package com.ruoyiliteflow.aicore.spi;

/**
 * 宿主进程内执行工具（例如 admin 直接查库），避免再打独立 MCP HTTP。
 * <p>core 不依赖业务模块；由宿主注册 Bean。platform 的 ToolExecutor 优先走本地。
 */
public interface LocalToolHandler
{
    boolean supports(ToolDescriptor tool);

    String execute(ToolDescriptor tool, String argumentsJson);
}
