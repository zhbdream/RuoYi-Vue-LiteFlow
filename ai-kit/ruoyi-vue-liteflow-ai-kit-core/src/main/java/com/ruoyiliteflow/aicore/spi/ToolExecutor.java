package com.ruoyiliteflow.aicore.spi;

/**
 * 执行已绑定工具。platform 提供 MCP/HTTP；无 Bean 时仅处理 echo。
 */
public interface ToolExecutor
{
    /**
     * @param tool 工具描述
     * @param argumentsJson 模型给出的 JSON 参数，可能为空
     * @return 给模型看的工具结果文本
     */
    String execute(ToolDescriptor tool, String argumentsJson);
}
