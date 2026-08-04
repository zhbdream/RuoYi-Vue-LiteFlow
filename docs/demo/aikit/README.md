# AI Kit Platform Demo（Phase A → C）

## 管理端菜单

依次执行 `sql/phase8_ai_kit_platform.sql` → `phase8b_ai_knowledge.sql` → `phase8c_ai_kit_enhance.sql` 后，侧栏 **AI能力**：

- 模型 / 工具 / 智能体 / 知识库 / **技能 / 记忆 / 上下文策略**

admin 已默认 `ruoyi.ai-kit.platform.enabled=true`。重新登录后可见菜单。

### Phase B 验收

1. 知识库「售后政策」种子文档启动后自动索引；智能体 `rag` 已绑定该库  
2. 智能体管理 → 试跑 `rag`，问「七天无理由怎么退？」应引用参考资料  
3. LiteFlow 链路 `aiKitAgentDemo` 试跑入参：`{"agentCode":"rag","message":"换货要几天？"}`

### Phase C 验收

1. 技能管理可见 `concise-zh`；`chat` 已绑定；试跑应更简洁  
2. 同一会话 `sessionId` 多轮试跑，记忆管理可见 turn 记录  
3. MCP（可选）：
   - `mvn -pl ruoyi-vue-liteflow-ai-kit-mcp -am spring-boot:run`
   - 或带 DB：`--spring.profiles.active=dynamic`
   - `POST /mcp/dynamic-tools` 注册后 `GET /mcp/tools` 可见；调用 `echo_ping` 回显

## 前置

1. 执行 `sql/phase8_ai_kit_platform.sql`（及 B/C 增量）
2. 配置 `DEEPSEEK_API_KEY`（或在 `/aikit/model` 写入加密 Key）
3. （可选）启动 MCP `:8090`
4. 启动 Boot（配置面）：

```bash
# 环境变量可覆盖库连接：AIKIT_DB_* 
mvn -pl ruoyi-vue-liteflow-ai-kit-boot -am spring-boot:run -Dspring-boot.run.profiles=platform
```

默认仍可不启 platform（无 DB）：内存 fallback 定义也可 `POST /agent/{code}/run`。

## 配置驱动试跑

```http
POST http://localhost:8091/agent/chat/run
Content-Type: application/json

{
  "message": "用一句话介绍你自己",
  "principal": "demo",
  "sessionId": "demo-1",
  "variables": {}
}
```

改库验证（需 platform profile）：

```sql
UPDATE ai_agent SET system_prompt = '你只能用英文回答，且每句以 [EN] 开头。' WHERE agent_code = 'chat';
```

再次调用 `/agent/chat/run`，输出应体现新提示词，无需改代码。

## 管理 API（platform 开启后）

| 方法 | 路径 |
|------|------|
| GET | `/aikit/model/list` |
| POST | `/aikit/model`（body 含明文 `apiKey`，仅写入时） |
| POST | `/aikit/model/test` |
| CRUD | `/aikit/tool/**` |
| CRUD | `/aikit/agent/**` |
| POST | `/aikit/agent/{agentCode}/run` |
| CRUD | `/aikit/skill/**` / `/aikit/memory/**` / `/aikit/context/**` |

列表/详情返回 `apiKeyMasked` / `hasApiKey`，**不回传明文 Key**。

## 兼容旧 Demo

仍可用：`/agent/chat/complete`、`/agent/risk/analyze`、`/agent/rag/ask`、`/agent/ops/chat`。
