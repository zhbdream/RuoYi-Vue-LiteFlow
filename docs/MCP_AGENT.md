# MCP Server + 独立 Agent 服务（Phase 7）

> 目标：把本系统能力封装为 **MCP Server** 对外提供；**Agent 与 LiteFlow 解耦**，按业务域拆成多个独立 Agent 服务。  
> LiteFlow 仅保留业务编排角色；需要智能能力时通过 HTTP / MCP 调用 Agent，而不是把 Agent 做成 `@LiteflowComponent`。

相关背景：

- 现有 AI 接入方式见 [AGENT.md](AGENT.md)、[LANGCHAIN.md](LANGCHAIN.md)、[CHAT.md](CHAT.md)
- 开放执行 API 见 [API.md](API.md)
- Demo 样例：[demo/mcp-ai-core/](demo/mcp-ai-core/)、[demo/agent/](demo/agent/)

---

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| **系统即 MCP** | 本仓库扮演 MCP **Server**（能力提供方） |
| **Agent 独立** | Agent 进程不依赖 LiteFlow EL / `NodeComponent` / `Slot` |
| **按域拆分** | MCP 与 Agent 均按业务域划分 |
| **LiteFlow 可选** | 编排中台继续服务非 AI 流程 |

---

## 2. 快速体验（开源演示）

```text
1) 配置环境变量 DEEPSEEK_API_KEY
2) IDEA 运行 McpServerApplication          → http://localhost:8090/
3) 按需运行 Agent*Application：
     Chat 8091 / Risk 8092 / RAG 8093 / Ops 8094
4) 浏览器打开各端口首页，或用 docs/demo/**/*.http
```

| 进程 | 端口 | 主类 | 浏览器 |
|------|------|------|--------|
| MCP Server | 8090 | `com.ruoyiliteflow.mcp.McpServerApplication` | http://localhost:8090/ |
| Chat Agent | 8091 | `...agentchat.AgentChatApplication` | http://localhost:8091/ |
| Risk Agent | 8092 | `...agentrisk.AgentRiskApplication` | http://localhost:8092/ |
| RAG Agent | 8093 | `...agentrag.AgentRagApplication` | http://localhost:8093/ |
| Ops Agent | 8094 | `...agentops.AgentOpsApplication` | http://localhost:8094/ |

默认 MCP Key：`ruoyi-mcp-key-change-me`（Header：`X-MCP-Api-Key`）。

架构：

```text
Agent(8091~8094)  --MCP HTTP-->  mcp-server(8090)
                                      ├─ ai-core Tools（chat / risk / rag / models）
                                      └─ lf-governance Tools（Demo 链路/日志/看板）
```

---

## 3. 模块

| Module | 职责 |
|--------|------|
| `ruoyi-vue-liteflow-ai-core` | Facade：Chat / Risk / RAG / Model（**无 LiteFlow**） |
| `ruoyi-vue-liteflow-mcp-server` | MCP HTTP/SSE + 简化 stdio + Playground |
| `ruoyi-vue-liteflow-agent-chat` | 会话助手 |
| `ruoyi-vue-liteflow-agent-risk` | 风控助手 |
| `ruoyi-vue-liteflow-agent-rag` | 售后知识问答助手 |
| `ruoyi-vue-liteflow-agent-ops` | 编排运维助手（治理 Tools + 总结） |

旧 LiteFlow AI 节点（`lc4jChat` / `lc4jRag`）已委托 Facade，Demo 链路仍可在完整 admin 下运行。

---

## 4. MCP Tools

### ai-core（默认开）

`list_models` / `get_default_model` / `chat_completion` / `risk_analyze` / `rag_ask` / `quota_status`

### lf-governance（默认开，Demo 数据）

`list_chains` / `get_chain` / `list_scripts` / `query_exec_logs` / `dashboard_summary`

> 当前为内置 Demo 数据，便于无 MySQL 演示；后续可对接真实 `lf_*` 表 / admin API。

### lf-runtime / sys

默认关（`execute_chain` 等高风险能力按需开启）。

---

## 5. 里程碑状态

### M0 — 文档

- [x] 架构与决策

### M1 — ai-core

- [x] Facade 模块
- [x] `lc4jChat` / `lc4jRag` 委托 Facade
- [ ] 独立单元测试（可选增强）

### M2 — mcp-server

- [x] HTTP/SSE + Playground
- [x] API Key 鉴权
- [x] 简化 stdio Launcher
- [x] Cursor 样例：[mcp.json.example](demo/mcp-ai-core/mcp.json.example)

### M3 — chat-agent

- [x] 独立进程 + 调试页
- [x] 经 MCP 调 `chat_completion`
- [ ] 后台「AI助手」菜单切流（仍走 `/liteflow/chat`，可选后续）
- [ ] `lf_chat_*` 多轮会话迁入 Agent（可选后续）

### M4 — risk / rag agent

- [x] `agent-risk` / `agent-rag` 独立服务 + 调试页

### M5 — governance + ops

- [x] `mcp-lf-governance` Demo Tools
- [x] `agent-ops` 拉取事实再总结

### M6 — runtime / sys

- [ ] 按需开启（默认关）

---

## 6. 决策记录（已确认）

| 决策项 | 已选 |
|--------|------|
| 代码组织 | 同仓多 module |
| MCP 传输 | HTTP/SSE + 简化 stdio |
| chat 会话表 | 首期继续 `lf_chat_*`（admin）；Agent 调试页为单轮 |
| 旧 AI 节点 | 保留 + 委托 Facade |
| runtime MCP | 默认关 |

---

## 7. 二次开发建议

1. 将 `risk_analyze` / `list_chains` 等 Tool 替换为业务系统真实接口  
2. 复制 `agent-*` 模块，按域扩展为客服 / 审批 / 运维等助手  
3. Agent **只依赖 MCP**，避免把智能逻辑塞回业务单体 Controller  

---

## 8. 参考

- [Model Context Protocol](https://modelcontextprotocol.io/)
- 本仓库：`IAi*Facade`、`McpServerApplication`、四个 `Agent*Application`
