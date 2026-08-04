# Phase 7：AI Kit（MCP + Agent）

> 与 LiteFlow 解耦。详见 [MCP_AGENT.md](../MCP_AGENT.md)。

## 启动

1. `McpServerApplication` → http://localhost:8090/
2. `AiKitBootApplication` → http://localhost:8091/

环境变量：`DEEPSEEK_API_KEY`；可选 `RUOYI_MCP_API_KEY`。

## 样例

| 文件 | 说明 |
|------|------|
| [../mcp-ai-core/](../mcp-ai-core/) | MCP HTTP / Cursor mcp.json |
| [agents.http](agents.http) | Boot 上 Chat/Risk/RAG/Ops |
