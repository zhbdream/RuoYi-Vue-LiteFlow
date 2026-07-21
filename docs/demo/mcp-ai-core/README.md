# mcp-ai-core 调用样例

独立进程默认端口 `8090`。鉴权头：`X-MCP-Api-Key`（默认 `ruoyi-mcp-key-change-me`，生产请改）。

## 启动

```bash
# 需先设置 DEEPSEEK_API_KEY
cd ruoyi-vue-liteflow-mcp-server
mvn spring-boot:run
```

stdio（开发 / Cursor）：

```bash
mvn -pl ruoyi-vue-liteflow-mcp-server -am package -DskipTests
java -cp ruoyi-vue-liteflow-mcp-server/target/ruoyi-vue-liteflow-mcp-server-3.9.2.jar \
  com.ruoyiliteflow.mcp.stdio.McpStdioLauncher
```

## HTTP

### 信息

```http
GET http://localhost:8090/mcp/info
```

### 列出 Tools

```http
GET http://localhost:8090/mcp/ai-core/tools
X-MCP-Api-Key: ruoyi-mcp-key-change-me
```

### 调用 chat_completion

```http
POST http://localhost:8090/mcp/ai-core/tools/chat_completion
Content-Type: application/json
X-MCP-Api-Key: ruoyi-mcp-key-change-me

{
  "systemPrompt": "你是助手",
  "userMessage": "用一句话介绍本系统",
  "principal": "demo"
}
```

### 调用 risk_analyze

见 [risk-analyze-request.json](risk-analyze-request.json)

### 调用 rag_ask

见 [rag-ask-request.json](rag-ask-request.json)

## agent-chat（8091）

先启动 mcp-server，再：

```bash
cd ruoyi-vue-liteflow-agent-chat
mvn spring-boot:run
```

```http
POST http://localhost:8091/agent/chat/complete
Content-Type: application/json

{
  "userMessage": "你好，介绍一下你自己"
}
```

本地直连 Facade（不经 MCP）：配置 `ruoyi.agent.chat.use-mcp=false`。
