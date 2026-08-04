# Changelog

## [v3.10.0] — 2026-08-04

### Phase 8：AI Kit 配置面（开源里程碑）

可配置、可嵌入的 Agent 能力层已落地（与 LiteFlow 解耦；LiteFlow 仅可选薄适配）。

#### 新增

- **模块**
  - `ruoyi-vue-liteflow-ai-kit-core`：Facade + `AgentRuntime` / Skills / Memory / ToolCatalog SPI
  - `ruoyi-vue-liteflow-ai-kit-platform`：模型 / 工具 / 智能体 / 知识库 / 技能 / 记忆 / 上下文 CRUD
  - `ruoyi-vue-liteflow-ai-kit-mcp`：MCP Server（静态 Tools + 动态注册）
  - `ruoyi-vue-liteflow-ai-kit-boot`：Chat/Risk/RAG/Ops 合一 + `POST /agent/{code}/run`
- **管理端菜单「AI能力」**：模型、工具、智能体、知识库、技能、记忆、上下文策略
- **LiteFlow 薄适配**：`aiKitAgentPrepare` / `aiKitAgent`，Demo 链路 `aiKitAgentDemo`
- **MCP 动态 Tool**：内存注册 API、可选 JDBC `profile=dynamic`、admin 保存 mcp 工具时推送
- **SQL**：`phase8_ai_kit_platform.sql` → `phase8b_ai_knowledge.sql` → `phase8c_ai_kit_enhance.sql`

#### 变更

- 原分散的 `ai-core` / `mcp-server` / 多 Agent 进程收敛为 `ai-kit-*` 模块族
- 旧独立 Agent Chat/Risk/RAG/Ops 工程移除，统一由 `ai-kit-boot` 提供

#### 文档

- [docs/AI_KIT_PLATFORM.md](docs/AI_KIT_PLATFORM.md)
- [docs/demo/aikit/README.md](docs/demo/aikit/README.md)

#### 升级提示

1. 已有库依次执行 phase8 / phase8b / phase8c，重新登录刷新菜单  
2. 配置 `DEEPSEEK_API_KEY` 或在「模型管理」写入 Key  
3. 日常使用 admin `:8080`；MCP `:8090`、boot `:8091` 可选  

### 非目标（本版不做）

- 面向终端用户的通用聊天产品  
- Kit 内替代 LiteFlow 的流程画布  
- 将 AI Kit 拆为独立仓库发布（可选，未做）

---

## 更早版本

详见 Git 历史：`feat: 新增 MCP Server…`（Phase 7）、内部 AI 助手（Phase 6）、LangChain/RAG（Phase 5）等。
