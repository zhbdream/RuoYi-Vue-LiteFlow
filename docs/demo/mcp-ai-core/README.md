# mcp-ai-core 调用样例

独立进程默认端口 `8090`。鉴权头：`X-MCP-Api-Key`（默认 `ruoyi-mcp-key-change-me`）。

## 启动

```bash
# 模块目录：ruoyi-vue-liteflow-ai-kit-mcp
# IDEA：运行 com.ruoyiliteflow.mcp.McpServerApplication
# 需设置 DEEPSEEK_API_KEY
```

stdio：

```bash
mvn -pl ruoyi-vue-liteflow-ai-kit-mcp -am package -DskipTests
java -cp ruoyi-vue-liteflow-ai-kit-mcp/target/ruoyi-vue-liteflow-ai-kit-mcp-3.9.2.jar \
  com.ruoyiliteflow.mcp.stdio.McpStdioLauncher
```

## HTTP

见 [mcp-tools.http](mcp-tools.http)。浏览器 Playground：http://localhost:8090/

## AI Kit Boot（8091）

先启动 MCP，再运行 `AiKitBootApplication`：

```http
POST http://localhost:8091/agent/chat/complete
Content-Type: application/json

{"userMessage":"你好"}
```

本地直连 Facade：`ruoyi.ai-kit.use-mcp=false`。
