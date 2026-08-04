# LiteFlow Demo 调用样例

本目录存放各 Demo 链路的请求 JSON，供 Postman / Swagger / curl 使用。

## 内部调用（需登录 Token）

```http
POST /liteflow/execute/{chainId}
Content-Type: application/json
Authorization: Bearer <token>

{ ... 见下方各 demo 文件 ... }
```

## 开放 API（外部系统，Phase 2D）

无需若依登录，在请求头携带 API Key（配置见 `application.yml` → `liteflow.open-api`）：

```http
POST /liteflow/open/execute/{chainId}
Content-Type: application/json
X-LiteFlow-Api-Key: ruoyi-liteflow-open-key-change-me

{ ... 与内部 execute 相同的 JSON 参数 ... }
```

也可使用若依 `Authorization: Bearer <token>`，且账号需具备 `liteflow:open:execute` 权限。

## 决策路由（Phase 3，仅内部 API）

```http
POST /liteflow/execute/route
Content-Type: application/json
Authorization: Bearer <token>

{ ... 见 routeDemo-request.json ... }
```

- 说明接口：`GET /liteflow/open/info`（无需鉴权）
- Swagger 分组：**LiteFlow开放API**

## Demo 清单

| chainId | 阶段 | 样例文件 | 说明 |
|---------|------|----------|------|
| helloChain | Phase 1 | helloChain-request.json | 入门三节点串行 |
| orderProcess | Phase 1 | orderProcess-request.json | 订单 IF/SWITCH |
| dynamicPricing | Phase 2 | dynamicPricing-request.json | 脚本 + 定价 |
| parallelAudit | Phase 2 | parallelAudit-request.json | WHEN 并行校验 |
| resilientNotify | Phase 2 | resilientNotify-request.json | CATCH/RETRY |
| batchProcess | Phase 2 | batchProcess-request.json | FOR 次数循环 |
| routeDemo | Phase 3 | routeDemo-request.json | 决策路由（`POST /liteflow/execute/route`） |
| fallbackDemo | Phase 3 | fallbackDemo-request.json | 声明式组件 + `@FallbackCmp`（`node("ghostNode")` 降级） |
| agentRiskDemo | Phase 4 | agentRiskDemo-request.json | DeepSeek Re-Act 风控 Agent（需 `DEEPSEEK_API_KEY`） |
| lc4jChatDemo | Phase 5 | lc4jChatDemo-request.json | LangChain4j AiServices + Tool |
| lc4jGraphDemo | Phase 5 | lc4jGraphDemo-request.json | LangGraph4j StateGraph 条件边风控 |
| lc4jRagDemo | Phase 5 | lc4jRagDemo-request.json | LangChain4j RAG 售后知识问答 |

## Phase 7：MCP + 独立 Agent

无需启动完整 admin / MySQL（LLM 需 `DEEPSEEK_API_KEY`）：

| 服务 | 端口 | 说明 |
|------|------|------|
| MCP Server | 8090 | http://localhost:8090/ Playground |
| AI Kit Boot | 8091 | Chat/Risk/RAG/Ops 合一调试页 |

样例：[mcp-ai-core/](mcp-ai-core/)、[agent/](agent/)。设计说明：[MCP_AGENT.md](../MCP_AGENT.md)。

**决策路由说明：** 非单 chain 执行。在链路管理中配置 `route_el` + `namespace`，通过 `executeRouteChain` 按入参命中规则。Demo5 包含 `newCustomerPromo`、`returningCustomerPromo` 两条链（namespace=`routeDemo`）。样例见 `routeDemo-request.json`。

**降级 Demo 说明：** 需 `liteflow.fallback-cmp-enable=true`。EL 为 `THEN(declareHello, node("ghostNode"), declareBye)`，`ghostNode` 不存在时走 `fallbackCommon`。

**LangChain Demo 说明：** 见 [LANGCHAIN.md](../LANGCHAIN.md)。Key 与 Demo7 共用「模型配置」/ `DEEPSEEK_API_KEY`。已有库执行 [sql/phase5_langchain.sql](../sql/phase5_langchain.sql)。

详细说明见项目 [README Demo 矩阵](../README.md#demo-矩阵)。
