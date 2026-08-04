# AI Kit 平台说明（Phase 8）

> 目标：在已规整的 **AI Kit**（与 LiteFlow 解耦）之上，提供可配置的 **模型 / 工具 / 知识库 / 智能体 / 技能 / 记忆 / 上下文** 管理能力，形成可抽到其他项目复用的「AI 架子」。  
> 定位为 **可嵌入的 Agent 能力层**，而非独立面向终端用户的通用聊天产品。

前置文档：[MCP_AGENT.md](MCP_AGENT.md)（Phase 7 运行时已落地）

**状态（本仓）：** Phase A / B / C 已落地；可选「独立仓库发布」未做。

---

## 0. 文档完备性说明

| 维度 | 状态 | 说明 |
|------|------|------|
| 产品边界 / 优先级 | **够用** | 八项能力有落点与 P0/P1/P2 |
| 模块划分 | **够用** | 只新增 `ai-kit-platform` |
| 表结构 / API 契约 | **已补强（下文）** | 可直接按 Phase A 开表与接口 |
| 安全 / 鉴权 / 配额 | **已补强** | 独立进程与 admin 宿主两种模式 |
| 验收标准 | **已补强** | 每阶段可测 |
| Skills / 记忆 / 上下文 | **已落地（Phase C）** | 见第 12 节验收 |

**结论：** Phase A / B / C 功能清单已在本仓落地；下文可作为接入说明与验收依据。

---

## 1. 背景与定位

| 维度 | 本仓库 AI Kit |
|------|----------------|
| 主业 | LiteFlow **业务编排中台**；AI 为可插拔能力层 |
| AI Kit | 可嵌入的能力库 + 可选管理面（模型 / 工具 / 知识库 / 智能体等） |
| 编排 | 业务编排继续用 LiteFlow EL + X6；Kit 内不重复造画布 |

**对外叙事：** 编排中台 + 可配置 Agent；AI Kit 可单独依赖，不强制 LiteFlow。

**不做：** 完整面向终端用户的通用聊天套件、一次做满全部增强菜单的深度产品、把 Agent 主路径绑回 `@LiteflowComponent`。

---

## 2. 现状（Phase 7）

| 模块 | 状态 | 说明 |
|------|------|------|
| `ruoyi-vue-liteflow-ai-kit-core` | 已有 | Facade + `AgentRuntime` |
| `ruoyi-vue-liteflow-ai-kit-mcp` | 已有 | MCP Server + Playground |
| `ruoyi-vue-liteflow-ai-kit-boot` | 已有 | Chat/Risk/RAG/Ops 合一示例（:8091）；`POST /agent/{code}/run` |
| `ruoyi-vue-liteflow-ai-kit-platform` | **Phase A 已落地** | 模型/工具/智能体 CRUD；`platform` profile 启用 |
| 管理面 CRUD | **菜单 + 页面已落地** | AI能力 → 模型 / 工具 / 智能体 |
| 知识库 | Demo | 内存向量 + `classpath:kb/*.md`（Phase B） |
| 模型 | 双轨 | `ai_model` + 回退 `lf_agent_model` / yml |

---

## 3. 目标架构

```text
┌─────────────────────────────────────────────────────────────┐
│ 若依 Admin（可选） 菜单「AI能力」                              │
│  模型 / 工具 / 知识库 / 智能体 / 技能·记忆·上下文         │
└───────────────────────────┬─────────────────────────────────┘
                            │ CRUD + 权限
┌───────────────────────────▼─────────────────────────────────┐
│ ruoyi-vue-liteflow-ai-kit-platform（新增）                     │
│  配置持久化 + 领域服务（无 LiteFlow）                          │
└───────────────────────────┬─────────────────────────────────┘
                            │ 读配置 / 组装运行
┌───────────────────────────▼─────────────────────────────────┐
│ ai-kit-core（运行时）          ai-kit-mcp（对外 Tools）         │
│ AgentRuntime.invoke(code)     静态 Tool + 动态 MCP 注册          │
└───────────────────────────┬─────────────────────────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
   ai-kit-boot 示例    其他业务项目         LiteFlow 薄适配
   (:8091 Demo)        只引 core/platform   HTTP 调 Agent（可选）
```

**原则：**

1. **配置与运行分离**：platform 管库表与 CRUD；core 管执行  
2. **Kit 零依赖 LiteFlow**；编排侧单向调用 Kit  
3. **其他项目**可只引 `ai-kit-core`（+ 可选 platform / mcp）

### 部署形态

| 形态 | 进程 | 说明 |
|------|------|------|
| Demo | mcp(:8090) + boot(:8091) | 与 Phase 7 一致；platform 可嵌入 boot 或 admin |
| 本仓完整 | admin(:8080) 引入 platform+core | 菜单管理配置；boot 可选 |
| 他项目 | 业务应用引入 core/platform | 自带数据源，执行 phase8 SQL |

---

## 4. 能力清单与优先级

| 能力项 | 落点 | 优先级 | MVP 范围 |
|------------------|------|--------|----------|
| 模型管理 | platform.model + core | **P0** | CRUD、默认模型、连通测试（发一条 chat） |
| 工具管理 | platform.tool + mcp | **P0** | CRUD、启用开关；local 先登记元数据，MCP 先静态映射 |
| 智能体管理 | platform.agent + core | **P0** | CRUD、绑定 model/tools/kb；`invoke(agentCode)` |
| 知识库管理 | platform.knowledge + core | **P1** | 库 CRUD、文档上传、切分入库、检索 |
| 编排管理 | 本仓 `liteflow` | **P1** | 薄节点/HTTP 调已配置 Agent；不新建 Kit 画布 |
| 技能 Skills | platform.skill | **P2 已落地** | 元数据 + `SkillResolver` SPI |
| 记忆 Memory | platform.memory | **P2 已落地** | `AgentMemoryStore` + 窗口策略 |
| 上下文 Context | platform.context | **P2 已落地** | `ai_context_policy` 绑定 Agent |

---

## 5. 新增模块：`ai-kit-platform`

### 5.1 Maven

```text
ruoyi-vue-liteflow-ai-kit-platform/
  pom.xml
  src/main/java/com/ruoyiliteflow/aikit/platform/
    domain/         # 实体
    mapper/
    service/        # IAiModelService / IAiToolService / IAiAgentService ...
    service/impl/
    controller/     # 可选：独立暴露时用；本仓也可放 admin
    runtime/        # ConfiguredAgentLoader → 委托 core.AgentRuntime
  src/main/resources/mapper/aikit/
```

| 依赖 | 说明 |
|------|------|
| 允许 | `ai-kit-core`、`common`；MyBatis/Spring 由宿主或本模块引入 |
| **禁止** | `liteflow`、`langchain`、`agent`（Re-Act 节点模块） |

### 5.2 命名

- Java：`com.ruoyiliteflow.aikit.platform.*`  
- 表：`ai_*`  
- SQL：`sql/phase8_ai_kit_platform.sql`  
- 权限标识建议：`aikit:model:*` / `aikit:tool:*` / `aikit:agent:*` / `aikit:knowledge:*`

---

## 6. 数据模型（Phase A 可建表）

> 字段类型按 MySQL 8；加密字段与现有 `lf_agent_model` 同样走 AES。

### 6.1 `ai_model`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| model_code | varchar(64) UK | 业务编码 |
| model_name | varchar(128) | 展示名 |
| provider | varchar(32) | deepseek / openai-compatible 等 |
| base_url | varchar(512) | |
| model | varchar(128) | 模型名如 deepseek-chat |
| api_key_enc | varchar(512) | 加密存储；接口不回传明文 |
| status | char(1) | 0 正常 1 停用 |
| is_default | char(1) | 1 默认 |
| daily_call_limit | int | 可空=用全局 |
| daily_token_limit | int | 可空 |
| create_by / create_time / update_by / update_time / remark | | 对齐若依 BaseEntity |

### 6.2 `ai_tool`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| tool_code | varchar(64) UK | |
| tool_name | varchar(128) | |
| tool_type | varchar(16) | `local` \| `mcp` |
| description | varchar(512) | |
| input_schema_json | text | JSON Schema，可空 |
| invoke_key | varchar(256) | local=Spring bean/方法键；mcp=tool 名 |
| mcp_server_key | varchar(64) | 如 `ai-core`，可空 |
| enabled | char(1) | 0/1 |
| 审计字段 | | 同 BaseEntity |

Phase A：`local` 可先只登记元数据，运行仍用代码内 Tool；`mcp` 映射到已有静态 MCP tool 名。

### 6.3 `ai_agent`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| agent_code | varchar(64) UK | 如 `chat` / `risk` / `demo-ops` |
| agent_name | varchar(128) | |
| system_prompt | text | |
| model_id | bigint | FK → ai_model.id，可空=默认模型 |
| temperature | decimal(3,2) | 默认 0.3 |
| enabled | char(1) | |
| 审计字段 | | |

### 6.4 关联表（推荐，Phase A 一起建）

```text
ai_agent_tool (agent_id, tool_id, sort)
ai_agent_knowledge (agent_id, kb_id, sort)   -- Phase B 再用，A 可先建空表
```

### 6.5 Phase B / C 表（占位，不阻塞 A）

- B：`ai_knowledge_base`、`ai_knowledge_doc`、`ai_knowledge_chunk`（或向量外置）  
- C：`ai_skill`、`ai_memory_item`、`ai_context_policy`

### 6.6 与现有表

| 现有 | 策略（已拍板建议） |
|------|-------------------|
| `lf_agent_model` | **新建 `ai_model`**；Phase A 用适配器：platform 优先读 `ai_model`，空则回退 `lf_agent_model`；后期单次迁移脚本 |
| `lf_chat_*` | 不动；Kit 记忆独立 |
| `lf_chain` | 不动 |

内置种子数据（SQL 或启动初始化）：`agent_code` = `chat` / `risk` / `rag` / `ops`，与 boot 现有 API 行为对齐。

---

## 7. API 契约（Phase A）

统一响应：若依 `AjaxResult`（`code` / `msg` / `data`）。

### 7.1 管理 API（嵌入 admin 时需登录权限）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/aikit/model/list` `/aikit/model` | 模型分页 / 新增 |
| PUT/DELETE | `/aikit/model` `/aikit/model/{ids}` | 修改 / 删除 |
| POST | `/aikit/model/test` | 连通测试（不落日志明文 Key） |
| CRUD | `/aikit/tool/**` | 工具 |
| CRUD | `/aikit/agent/**` | 智能体 |
| POST | `/aikit/agent/{agentCode}/run` | 管理端试跑（可选） |

### 7.2 Boot 运行 API（:8091，API Key 或开放）

| 方法 | 路径 | 说明 |
|------|------|------|
| 保留 | `/agent/chat/complete` 等 | 兼容 Demo |
| **新增** | `POST /agent/{agentCode}/run` | 配置驱动入口 |

请求体示例：

```json
{
  "message": "用户输入",
  "principal": "demo",
  "variables": {}
}
```

响应示例：

```json
{
  "code": 200,
  "data": {
    "agentCode": "risk",
    "content": "...",
    "model": "deepseek-chat",
    "toolTrace": []
  }
}
```

---

## 8. 运行时约定

```text
AgentRuntime.invoke(agentCode, request)
  1. 加载 ai_agent（enabled）及 model / tools / kb
  2. 解析凭证 → ChatModel（禁止日志打印 Key）
  3. 挂载已启用 Tools
  4. 若绑定 KB → 检索拼入上下文（Phase B；A 可跳过）
  5. 调用 LLM → AgentRunResult
```

**core 新增接口建议：**

```text
com.ruoyiliteflow.aicore.runtime.AgentRuntime
com.ruoyiliteflow.aicore.runtime.AgentRunRequest
com.ruoyiliteflow.aicore.runtime.AgentRunResult
com.ruoyiliteflow.aicore.runtime.AgentDefinition  // 可由 platform 组装后传入，避免 core 依赖 platform
```

依赖方向：**platform → core**（单向）。core 只认 `AgentDefinition` DTO，不依赖 platform Mapper。

**MCP：** Phase C 支持动态注册（内存 API / JDBC `dynamic` / admin 推送）。

---

## 9. 安全、配额、观测

| 项 | Phase A 要求 |
|----|----------------|
| API Key | 独立进程沿用 `X-MCP-Api-Key`；admin 走若依 JWT + `@PreAuthorize` |
| 模型 Key | 仅存密文；列表/详情脱敏；测试接口不返回明文 |
| 配额 | 复用/扩展现有配额 SPI；dimension 建议 `aikit:{agentCode}` |
| 审计 | CRUD 写操作日志（若依 operlog 或简单表） |
| 观测 | 结构化日志：agentCode、耗时、成功失败；禁止 dump 完整 prompt+key |

---

## 10. 管理端（本仓）

菜单分组：**AI能力**（执行 `sql/phase8_ai_kit_platform.sql` 后可见）

| 菜单 | 阶段 | 页面要点 |
|------|------|----------|
| 模型管理 | **A 已落地** | 列表、新增编辑、设默认、测试连通 |
| 工具管理 | **A 已落地** | 列表、启用、类型筛选 |
| 智能体管理 | **A 已落地** | 绑定模型/工具、试跑对话框 |
| 知识库管理 | **B 已落地** | 库与文档、上传、重建索引 |
| 技能 / 记忆 / 上下文 | **C 已落地** | Skills CRUD、记忆条目、上下文策略 |
| 编排 | **B 已落地** | 链路 `aiKitAgentDemo` → `AgentRuntime` |

前端目录：`ruoyi-vue-liteflow-ui/src/views/aikit/`  
权限：`aikit:model:*` / `aikit:tool:*` / `aikit:agent:*` / `aikit:knowledge:*` / `aikit:skill:*` / `aikit:memory:*` / `aikit:context:*`  
admin 已引入 `ai-kit-platform`，并默认 `ruoyi.ai-kit.platform.enabled=true`。

---

## 11. 与 LiteFlow 的衔接（Phase B）

| 方式 | 说明 |
|------|------|
| 推荐 | 通用 Cmp / HTTP：配置 `agentCode` + boot/admin URL |
| 不推荐 | 每个业务 Agent 再写一个 `NodeComponent` 当主交付 |

编排管理 = 现有链路编排 + 「调用已配置 Agent」，**不在 Kit 内再造画布**。

---

## 12. 实施里程碑与验收

### Phase A — 配置驱动骨架

- [x] 新建 `ai-kit-platform` + `sql/phase8_ai_kit_platform.sql`
- [x] `ai_model` / `ai_tool` / `ai_agent` / 关联表 CRUD API
- [x] core：`AgentRuntime` + Definition DTO
- [x] boot：`POST /agent/{agentCode}/run`；种子 Agent：chat/risk/rag/ops
- [x] 模型 Key 脱敏 + 连通测试
- [x] 文档与 `docs/demo/aikit/` HTTP 样例

**验收：**

1. 无 LiteFlow、不启完整 admin（或仅 DB）下，boot + MCP 可按 `agentCode` 跑通
2. 改库中某 Agent 的 `system_prompt` 后，无需改代码即可影响输出
3. 接口不返回明文 API Key

启动配置面：`--spring.profiles.active=platform`（见 [demo/aikit](demo/aikit/README.md)）。

### Phase B — 知识库 + 编排联动

- [x] 知识库上传 / 切分 / 检索  
- [x] 向量存储 SPI（`KnowledgeRetriever` + 内存 InMemory/All-MiniLM）  
- [x] LiteFlow 薄适配节点（`aiKitAgentPrepare` / `aiKitAgent`，链路 `aiKitAgentDemo`）  
- [x] admin 四菜单可用（模型 / 工具 / 智能体 / 知识库）  

**验收：** 绑定 KB 的 Agent 能答出知识库内政策类问题；链路试跑能调到 Kit Agent。

增量 SQL：`sql/phase8b_ai_knowledge.sql`（先执行 phase8）。

### Phase C — 增强

- [x] Skills / 记忆 / 上下文（表 + CRUD + `AgentRuntime` SPI）  
- [x] MCP Tool 动态注册（内存 API + 可选 JDBC `profile=dynamic` + admin 推送）  
- [ ] （可选）独立仓库发布  

增量 SQL：`sql/phase8c_ai_kit_enhance.sql`（先 phase8 → phase8b）。

**验收：**

1. 智能体绑定技能后，试跑输出体现 skill prompt；同 `sessionId` 多轮可见记忆窗口  
2. MCP：`GET /mcp/tools` 含动态工具；`POST /mcp/dynamic-tools` 可注册；`echo_ping` 可调用  
3. admin 保存 mcp 工具后（配置 `ruoyi.ai-kit.mcp.register-url`）推送到 MCP 内存表  

### 非目标

- 做成完整面向终端用户的通用聊天产品  
- Kit 内替代 LiteFlow 的流程画布  
- 强制废弃旧 Demo 链路  

---

## 13. 其他项目接入

```text
业务项目
  ├── 依赖 ai-kit-core
  ├── （可选）ai-kit-platform + phase8 SQL
  ├── （可选）ai-kit-mcp
  └── 配置数据源、注册业务 Tool、写入 ai_agent
```

不需要：`liteflow`、本仓 admin、X6。

---

## 14. 决策记录（已建议拍板）

| 项 | 决定 |
|----|------|
| 模型表 | 新建 `ai_model`，过渡期回退读 `lf_agent_model` |
| Phase A UI | **已补菜单 + CRUD**（含知识库 / 技能 / 记忆 / 上下文） |
| 知识库 | Phase B 已落地；存储 SPI 可插拔 |
| 编排 | 不进 platform；薄适配节点已落地 |
| 模块 | 只新增 **一个** `ai-kit-platform` |
| core←platform | **禁止** core 依赖 platform；用 Definition DTO 注入 |

本文即 Phase 8 实施说明与验收清单（A/B/C 已完成）。

---

## 15. 参考

- [MCP_AGENT.md](MCP_AGENT.md) — Phase 7 运行时  
- [Model Context Protocol](https://modelcontextprotocol.io/)  
- 本仓库：`ai-kit-core` / `ai-kit-mcp` / `ai-kit-boot`
