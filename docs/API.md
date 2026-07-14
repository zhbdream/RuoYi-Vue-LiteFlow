# LiteFlow API 文档

> RuoYi-Vue-LiteFlow 对外与内部执行 API 说明。  
> 基础地址示例：`http://localhost:8080`（以实际部署为准）

---

## 一、API 分组

| 分组 | 路径前缀 | 说明 | Swagger 分组 |
|------|----------|------|--------------|
| 内部执行 | `/liteflow/execute` | 若依登录 + 菜单权限 | `liteflow` |
| 开放执行 | `/liteflow/open` | API Key 或 Token | `liteflow-open` |
| 链路管理 | `/liteflow/chain` | CRUD / 发布 / 克隆等 | `liteflow` |
| 编排器 | `/liteflow/el` | EL 校验 / 在线调试 | `liteflow` |
| 组件 | `/liteflow/component` | 可编排组件列表 | `liteflow` |
| 执行日志 | `/liteflow/log` | 历史记录与步骤 | `liteflow` |
| 监控仪表盘 | `/liteflow/dashboard` | 成功率 / 趋势 / Top N | `liteflow` |

Swagger UI：`/swagger-ui.html` → 选择 **LiteFlow编排** 或 **LiteFlow开放API**。

---

## 二、开放执行 API（外部系统集成）

### 2.1 获取开放 API 信息

无需鉴权。

```http
GET /liteflow/open/info
```

**响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "enabled": true,
    "headerName": "X-LiteFlow-Api-Key",
    "executePath": "POST /liteflow/open/execute/{chainName}",
    "auth": "X-LiteFlow-Api-Key 或 Bearer Token（需 liteflow:open:execute 权限）"
  }
}
```

### 2.2 执行链路

```http
POST /liteflow/open/execute/{chainName}
Content-Type: application/json
X-LiteFlow-Api-Key: <your-api-key>
```

或使用若依 JWT：

```http
POST /liteflow/open/execute/{chainName}
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数：**

| 参数 | 说明 |
|------|------|
| `chainName` | 链路 ID，如 `helloChain`、`orderProcess` |

**请求体（可选）：** JSON 对象，作为 LiteFlow 上下文入参。

**helloChain 示例：**

```bash
curl -X POST "http://localhost:8080/liteflow/open/execute/helloChain" \
  -H "Content-Type: application/json" \
  -H "X-LiteFlow-Api-Key: ruoyi-liteflow-open-key-change-me" \
  -d '{"name": "RuoYi"}'
```

**orderProcess 示例：**

```bash
curl -X POST "http://localhost:8080/liteflow/open/execute/orderProcess" \
  -H "Content-Type: application/json" \
  -H "X-LiteFlow-Api-Key: ruoyi-liteflow-open-key-change-me" \
  -d '{
    "userId": 1001,
    "skuId": "SKU-001",
    "quantity": 2,
    "payType": "wechat",
    "couponCode": "SAVE10"
  }'
```

**成功响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": true,
    "code": "200",
    "message": "success",
    "chainId": "helloChain",
    "requestId": "a1b2c3d4-...",
    "executeStepStr": "helloA==>helloB==>helloC",
    "executeStepStrWithTime": "...",
    "contextData": { },
    "logId": 42,
    "failedNodeId": null
  }
}
```

**失败响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": false,
    "code": "500",
    "message": "component[sendNotify] execute error...",
    "chainId": "resilientNotify",
    "requestId": "...",
    "executeStepStr": "initNotify==>sendNotify",
    "failedNodeId": "sendNotify",
    "logId": 43
  }
}
```

> 说明：HTTP 层仍返回 `code: 200`，业务成败看 `data.success`。

### 2.3 鉴权方式

| 方式 | 请求头 | 说明 |
|------|--------|------|
| **API Key（推荐）** | `X-LiteFlow-Api-Key: <key>` | 服务端配置项，适合外部系统 |
| **Bearer Token** | `Authorization: Bearer <jwt>` | 需用户具备 `liteflow:open:execute` 权限 |

**配置项**（`application.yml`）：

```yaml
liteflow:
  # 生产只读：true 时禁止链路/脚本/版本回滚等写操作，执行与试跑仍可用
  readonly:
    enabled: false
    message: 当前环境为只读模式，禁止修改规则/脚本
  open-api:
    enabled: true
    api-key: ruoyi-liteflow-open-key-change-me   # 生产环境务必修改
    header-name: X-LiteFlow-Api-Key
    allow-agent-chains: false                    # 含 Agent 链路默认禁止开放执行
```

前端可通过 `GET /liteflow/config` 读取 `{ readonly, readonlyMessage, agentConfigured, openApiAllowAgentChains }`。

### Webhook 回调

链路执行（入库后）可异步 HTTP POST 结果 JSON。

```yaml
liteflow:
  webhook:
    enabled: false          # 全局开关；链路填写 webhookUrl 时可不依赖 enabled
    url: https://example.com/hook
    only-on-failure: false
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
```

优先级：**链路 `webhookUrl` > 全局 `liteflow.webhook.url`（需 enabled）**。EL 在线调试不回调。

Payload 字段示例：`event`、`chainName`、`requestId`、`success`、`durationMs`、`executeStepStr`、`failedNodeId`、`logId`、`param`。

### 定时执行（Quartz）

在「系统监控 → 定时任务」中配置调用目标（bean 位于 `com.ruoyiliteflow.quartz.task`）：

| invokeTarget 示例 | 说明 |
|-------------------|------|
| `liteFlowTask.executeByName('helloChain')` | 无参执行链路 |
| `liteFlowTask.executeByName('orderProcess', '{"userId":1001,"skuId":"SKU-001","quantity":2,"payType":"wechat"}')` | 带 JSON 参数 |

执行结果写入 `lf_exec_log`，`create_by` 为 `quartz`。

### 2.4 执行前置条件

链路必须同时满足：

- 已发布（`draft_flag = 0`）
- 状态正常（`status = 0`）
- 启用（`enable = 1`）

否则返回业务异常：`链路未启用或处于草稿/停用状态`。

### 2.5 链路级执行权限

若某链路在 **链路权限** 中配置了角色限制：

- **API Key 调用**：不受链路级权限限制（视为系统集成账号）
- **Token 调用**：当前用户角色须在「可执行」列表中

未配置链路权限时，仅校验菜单权限 `liteflow:open:execute` 或 `liteflow:execute`。

---

## 三、内部执行 API（后台 / 编排器试跑）

```http
POST /liteflow/execute/{chainName}
Authorization: Bearer <token>
Content-Type: application/json
```

**权限：** `liteflow:execute`

请求体、响应结构与开放 API 相同。执行记录写入 `lf_exec_log`，`create_by` 为当前登录用户名。

**链路级权限：** 若已配置，当前用户角色须具备「可执行」权限（admin 不受限）。

### 3.0 流式试跑（SSE，Phase 4）

```http
POST /liteflow/execute/stream/{chainName}
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream
```

**权限：** `liteflow:execute`

服务端推送 `text/event-stream` 事件：

| event | 说明 |
|-------|------|
| `agent.reasoning` | Agent 推理/回复增量 |
| `agent.tool_result` | 工具调用结果 |
| `agent.result` | 本轮最终 Agent 文本 |
| `done` | 整链结果（同同步试跑的 data） |
| `error` | 执行异常 |

前端：链路管理 → 试跑 → 打开「SSE（Agent 推理过程）」。

### 3.1 决策路由执行（Phase 3）

按 `namespace` 遍历带 `route_el` 的链路，命中规则并行执行（LiteFlow `executeRouteChain`）。

```http
POST /liteflow/execute/route
Authorization: Bearer <token>
Content-Type: application/json
```

**权限：** `liteflow:execute`

**请求体：**

```json
{
  "namespace": "routeDemo",
  "contextClass": "com.ruoyiliteflow.liteflow.domain.context.RouteUserContext",
  "param": {
    "userType": "NEW"
  }
}
```

| 字段 | 说明 |
|------|------|
| `namespace` | 决策路由命名空间，如 `routeDemo`；可省略（遍历全部带 route 的链） |
| `contextClass` | 上下文 Class 全限定名，可选 |
| `param` | 入参 JSON，会注入上下文 |

**响应示例：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "namespace": "routeDemo",
    "hitCount": 1,
    "results": [
      {
        "success": true,
        "chainId": "newCustomerPromo",
        "requestId": "...",
        "executeStepStr": "newCustomerWelcome==>newCustomerDiscount",
        "contextData": { "userType": "NEW", "message": "欢迎新客..." },
        "logId": 10
      }
    ]
  }
}
```

> 每条命中规则单独写入 `lf_exec_log`，`chainName` 为实际命中的链路 ID。

**curl 示例：**

```bash
curl -X POST "http://localhost:8080/liteflow/execute/route" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "routeDemo",
    "contextClass": "com.ruoyiliteflow.liteflow.domain.context.RouteUserContext",
    "param": { "userType": "RETURNING" }
  }'
```

---

## 四、EL 在线调试 API（Phase 3）

直接执行一段 EL，不入库、不依赖已发布链路（LiteFlow `execute2RespWithEL`）。

```http
POST /liteflow/el/execute
Authorization: Bearer <token>
Content-Type: application/json
```

**权限：** `liteflow:execute`

**请求体：**

```json
{
  "elData": "THEN(helloA, helloB, helloC);",
  "param": { "name": "RuoYi" },
  "contextClass": null
}
```

| 字段 | 说明 |
|------|------|
| `elData` | EL 表达式（必填） |
| `param` | 入参 JSON，可选 |
| `contextClass` | 上下文 Class 全限定名，可选 |

**响应结构**与单链路执行相同（`LiteFlowExecuteResultVo`）。执行记录 `chainName` 记为 `(EL调试)`。

**EL 校验（已有）：**

```http
POST /liteflow/el/validate
```

**权限：** `liteflow:chain:edit`

---

## 五、监控仪表盘 API（Phase 3）

```http
GET /liteflow/dashboard?days=7
Authorization: Bearer <token>
```

**权限：** `liteflow:dashboard:view`

**查询参数：**

| 参数 | 说明 | 默认 |
|------|------|------|
| `days` | 统计近 N 天（1~90） | 7 |

**响应 `data` 字段：**

| 字段 | 说明 |
|------|------|
| `totalCalls` | 总调用次数 |
| `successCount` / `failCount` | 成功 / 失败次数 |
| `successRate` | 成功率（%） |
| `avgDurationMs` | 平均耗时 |
| `trend` | 按日趋势 `[{ statDate, total, successCount, failCount }]` |
| `chainStats` | 各链路统计 Top 15 |
| `failTop` | 失败 Top 10（按链路 + 错误信息聚合） |
| `slowTop` | 慢调用 Top 10（按 `durationMs` 排序） |

> 表结构与菜单已包含在全量脚本 [sql/ry-vue.sql](../sql/ry-vue.sql) 中。

---

## 六、执行日志 API

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/liteflow/log/list` | GET | `liteflow:log:list` | 分页列表 |
| `/liteflow/log/{id}` | GET | `liteflow:log:query` | 详情（含步骤、失败节点） |
| `/liteflow/log/request/{requestId}` | GET | `liteflow:log:query` | 按 requestId 查询 |

**列表查询参数：** `requestId`、`chainName`、`success`（1/0）、时间范围

**详情关键字段：**

| 字段 | 说明 |
|------|------|
| `executeStepStr` | 执行步骤，如 `a==>b==>c` |
| `failedNodeId` | 失败时最后执行节点 ID |
| `paramJson` | 请求参数 |
| `contextJson` | 上下文结果 |
| `durationMs` | 耗时 |

前端支持从日志 **「定位失败」** 跳转编排器，绿色高亮已执行步骤、红色高亮失败节点。

---

## 七、链路权限 API

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/liteflow/chain/permission/{chainName}` | GET | `liteflow:chain:permission` | 查询链路角色权限 |
| `/liteflow/chain/permission` | POST | `liteflow:chain:permission` | 保存（全量替换） |

**保存请求体：**

```json
{
  "chainName": "orderProcess",
  "permissions": [
    { "roleId": 2, "canExecute": "1", "canEdit": "0" },
    { "roleId": 3, "canExecute": "0", "canEdit": "1" }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `canExecute` | `1` 可试跑/执行 |
| `canEdit` | `1` 可保存草稿、发布、编排器保存 |

传空 `permissions` 数组 = 清除限制，恢复仅菜单权限控制。

> 权限相关表结构已包含在全量脚本 [sql/ry-vue.sql](../sql/ry-vue.sql) 中。

---

## 八、常用内部 API 速查

| 接口 | 方法 | 权限 |
|------|------|------|
| `/liteflow/chain/list` | GET | `liteflow:chain:list` |
| `/liteflow/chain/{id}` | GET | `liteflow:chain:query` |
| `/liteflow/chain/publish/{id}` | POST | `liteflow:chain:edit` |
| `/liteflow/chain/reload/{chainName}` | POST | `liteflow:chain:reload` |
| `/liteflow/component/list` | GET | `liteflow:editor:view` |
| `/liteflow/el/validate` | POST | `liteflow:chain:edit` |
| `/liteflow/el/execute` | POST | `liteflow:execute` |
| `/liteflow/execute/route` | POST | `liteflow:execute` |
| `/liteflow/dashboard` | GET | `liteflow:dashboard:view` |

---

## 九、Demo 样例请求

样例 JSON 位于 `docs/demo/`：

| 链路 | 文件 |
|------|------|
| helloChain | `helloChain-request.json` |
| orderProcess | `orderProcess-request.json` |
| dynamicPricing | `dynamicPricing-request.json` |
| parallelAudit | `parallelAudit-request.json` |
| resilientNotify | `resilientNotify-request.json` |
| batchProcess | `batchProcess-request.json` |
| routeDemo | `routeDemo-request.json`（决策路由，走 `/liteflow/execute/route`） |

开放 API 调用时，请求体使用各文件中 `param` 字段的内容。决策路由 Demo 见 `routeDemo-request.json` 完整请求体。

---

## 十、错误码说明

| HTTP / Ajax code | 场景 |
|------------------|------|
| 401 | 开放 API 未携带有效 Key 或 Token |
| 403 | 开放 API 功能未启用（`liteflow.open-api.enabled=false`） |
| 500 | 含 Agent 的链路走开放 API 且 `allow-agent-chains=false`（业务 ServiceException） |
| 500 + msg | 业务异常：链路不存在、无权限、未发布等 |

---

## 十一、安全建议

1. **生产环境** 修改默认 `api-key`，并通过环境变量或外部配置注入
2. API Key 仅用于服务端调用，不要写入前端代码
3. 对开放 API 网关层增加 IP 白名单或限流（可选）
4. 链路级权限 + 菜单权限组合，实现「可编排不可执行」分权
