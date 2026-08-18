# MCP Server + 独立 Agent（AI Kit）

> 目标：与 LiteFlow **解耦**的可复用 AI 架子（AI Kit）：MCP 对外提供能力，Agent 示例单进程消费。  
> LiteFlow 仅保留业务编排；需要智能时通过 HTTP / MCP 调用即可。

相关背景：[AGENT.md](AGENT.md)、[LANGCHAIN.md](LANGCHAIN.md)、[CHAT.md](CHAT.md)、[API.md](API.md)  
Demo：[demo/mcp-ai-core/](demo/mcp-ai-core/)、[demo/agent/](demo/agent/)

---

## 1. 模块

| Module | 职责 |
|--------|------|
| `ruoyi-vue-liteflow-ai-kit-core` | Facade：Chat / Risk / RAG / Model + AgentRuntime（**无 LiteFlow**） |
| `ruoyi-vue-liteflow-ai-kit-mcp` | MCP HTTP/SSE + stdio + Playground |
| `ruoyi-vue-liteflow-ai-kit-boot` | Chat / Risk / RAG / Ops **合一**示例进程 |
| `ruoyi-vue-liteflow-ai-kit-platform` | 模型 / 工具 / 智能体配置面（可嵌入 admin） |

目录：`ai-kit/`。artifactId 不变；命令行请用 `-pl :ruoyi-vue-liteflow-ai-kit-mcp`（artifactId 前加冒号）。

可抽到其他项目：只依赖 `ai-kit-core`（+ 可选 `ai-kit-mcp` / `ai-kit-platform`），不必引入本仓库的 `liteflow` / `admin`。

---

## 2. 快速体验

```text
1) 配置 DEEPSEEK_API_KEY
2) 运行 McpServerApplication     → http://localhost:8090/
3) 运行 AiKitBootApplication     → http://localhost:8091/
```

| 进程 | 端口 | 主类 |
|------|------|------|
| MCP | 8090 | `com.ruoyiliteflow.mcp.McpServerApplication` |
| Agent Boot | 8091 | `com.ruoyiliteflow.aikit.boot.AiKitBootApplication` |

默认 MCP Key：`ruoyi-mcp-key-change-me`（`X-MCP-Api-Key`）。

```text
AiKitBoot(:8091)  --MCP HTTP-->  ai-kit-mcp(:8090)
                                    ├─ ai-core Tools
                                    └─ lf-governance Tools（Demo 数据）
```

Boot API：

- `POST /agent/chat/complete`
- `POST /agent/risk/analyze`
- `POST /agent/rag/ask`
- `POST /agent/ops/chat`

---

## 3. MCP Tools

**ai-core：** `list_models` / `get_default_model` / `chat_completion` / `risk_analyze` / `rag_ask` / `quota_status`  

**lf-governance（Demo）：** `list_chains` / `get_chain` / `list_scripts` / `query_exec_logs` / `dashboard_summary`  

**lf-runtime / sys：** 默认关。

---

## 4. 与 LiteFlow 的关系

- `lc4jChat` / `lc4jRag` 节点委托 `ai-kit-core` Facade（兼容旧 Demo）
- 新能力优先走 AI Kit，**不要**再拆多个 `agent-*` Maven 模块
- 编排侧若要调用 Agent：HTTP 节点指向 `:8091` 即可

日常管理走 admin `:8080` 菜单 **AI能力**（模型 / 工具 / 智能体 / 知识库 / 技能 / 记忆 / 上下文 / 助手），无需单独启 boot。独立进程仅在对接外部 MCP 客户端时需要。

---

## 5. 二次开发建议

1. 将 Tools 换成业务真实接口  
2. 在 `ai-kit-boot` 内扩展包，或只依赖 `ai-kit-core` 自建启动器  
3. Agent **只依赖 MCP / Facade**，避免绑回业务单体与 LiteFlow  

---

## 6. 参考

- [Model Context Protocol](https://modelcontextprotocol.io/)
- 本仓库：`IAi*Facade`、`McpServerApplication`、`AiKitBootApplication`
