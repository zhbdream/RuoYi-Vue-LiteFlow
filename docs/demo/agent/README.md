# Phase 7：MCP + 独立 Agent

> 系统能力封装为 MCP；Agent 与 LiteFlow 解耦。详见 [MCP_AGENT.md](../MCP_AGENT.md)。

## 启动

1. `McpServerApplication` → http://localhost:8090/ （浏览器 Playground）
2. 按需启动 Agent：
   - Chat `8091` → http://localhost:8091/
   - Risk `8092` → http://localhost:8092/
   - RAG `8093` → http://localhost:8093/
   - Ops `8094` → http://localhost:8094/

环境变量：`DEEPSEEK_API_KEY`（调 LLM 必填）、可选 `RUOYI_MCP_API_KEY`。

## 样例文件

| 文件 | 说明 |
|------|------|
| [mcp-ai-core/](../mcp-ai-core/) | MCP HTTP / Cursor mcp.json |
| [agents.http](agents.http) | 四个 Agent 的 IDEA HTTP 样例 |
