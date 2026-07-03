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

**决策路由说明：** 非单 chain 执行。在链路管理中配置 `route_el` + `namespace`，通过 `executeRouteChain` 按入参命中规则。Demo5 包含 `newCustomerPromo`、`returningCustomerPromo` 两条链（namespace=`routeDemo`）。样例见 `routeDemo-request.json`。

详细说明见项目 [README Demo 矩阵](../README.md#demo-矩阵)。
