# LangChain4j / LangGraph4j（Phase 5）

在 LiteFlow 编排链路中接入 **LangChain4j**（ChatModel / AiServices / Tool / RAG）与 **LangGraph4j**（StateGraph / 条件边），与现有 LiteFlow Re-Act Agent（Demo7）**并存**。

能力分层：

| 框架 | 本平台落点 | 解决什么 |
|------|-----------|----------|
| LangChain4j | 节点 `lc4jChat` | 统一 ChatModel、AiServices、Tool Calling |
| LangGraph4j | 节点 `lc4jGraph` | 有状态、可循环/分支的 Agent 图 |
| LangChain4j RAG | 节点 `lc4jRag` | 知识库检索增强生成 |
| LiteFlow | 外层 EL `THEN(...)` | 业务编排、权限、配额、审计、可视化 |

## 前置条件

1. JDK 17+
2. 与 Agent 相同的 DeepSeek / OpenAI 兼容 Key（「AI能力 → 模型管理」默认模型，或 `DEEPSEEK_API_KEY`）
3. 已导入含 Demo8/9/10 的 [sql/ry-vue.sql](../sql/ry-vue.sql)

## 模块

`ruoyi-vue-liteflow-langchain`，由 `ruoyi-vue-liteflow-admin` 引入。

依赖版本（父 POM）：

- `dev.langchain4j:langchain4j` / `langchain4j-open-ai` **1.17.2**
- `org.bsc.langgraph4j:langgraph4j-core` **1.8.20**
- `dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2-q` **1.17.2-beta27**（本地 Embedding）

## Demo8：`lc4jChatDemo`（LangChain4j）

```text
THEN(lc4jPrepare, lc4jChat, lc4jNotify)
```

| 节点 | 说明 |
|------|------|
| `lc4jPrepare` | 入参写入 `Lc4jRiskContext` |
| `lc4jChat` | AiServices + `@Tool(read_order_risk_context)` |
| `lc4jNotify` | 解析风险等级 |

试跑：

```http
POST /liteflow/execute/lc4jChatDemo
Content-Type: application/json

{
  "orderId": "ORD-LC4J-1001",
  "userId": 2001,
  "userType": "NEW",
  "amount": 2599.00,
  "scene": "checkout"
}
```

样例：[demo/lc4jChatDemo-request.json](demo/lc4jChatDemo-request.json)

## Demo9：`lc4jGraphDemo`（LangGraph4j）

```text
THEN(lc4jPrepare, lc4jGraph, lc4jNotify)
```

图结构（在单节点 `lc4jGraph` 内）：

```text
START -> gatherFacts -> llmAnalyze --HIGH--> escalate -> END
                                 \--else--> pass     -> END
```

| 图节点 | 说明 |
|--------|------|
| `gatherFacts` | 调用业务 Tool 读订单事实 |
| `llmAnalyze` | LangChain4j `ChatModel.chat` 出等级与理由 |
| `escalate` / `pass` | 条件边按 `riskLevel` 分流，写轨迹到 `graphTrace` |

试跑：

```http
POST /liteflow/execute/lc4jGraphDemo
Content-Type: application/json

{
  "orderId": "ORD-GRAPH-1001",
  "userId": 2001,
  "userType": "NEW",
  "amount": 9999.00,
  "scene": "checkout"
}
```

样例：[demo/lc4jGraphDemo-request.json](demo/lc4jGraphDemo-request.json)

试跑洞察示意（风险结论 + LangGraph 轨迹）：

<p align="center">
  <img src="img/链路试跑LangGraph4j%20状态图风控.png" alt="LangGraph4j 试跑" width="720" />
</p>

## Demo10：`lc4jRagDemo`（RAG 售后问答）

```text
THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify)
```

| 节点 | 说明 |
|------|------|
| `lc4jRagPrepare` | 读取 `question` 入参 |
| `lc4jRag` | 本地 Embedding 检索 `classpath:kb/*.md` + ChatModel 生成答案 |
| `lc4jRagNotify` | 整理命中数与答案 |

知识库文件（模块内置，启动时入库内存向量库）：

- `kb/after-sales-return.md` 退货
- `kb/after-sales-exchange.md` 换货
- `kb/after-sales-shipping.md` 运费时效

试跑：

```http
POST /liteflow/execute/lc4jRagDemo
Content-Type: application/json

{
  "question": "下单后第5天衣服尺码不合适，可以退货或换货吗？运费谁承担？"
}
```

样例：[demo/lc4jRagDemo-request.json](demo/lc4jRagDemo-request.json)

结果关注字段：`answer`、`retrievedContext`、`hitCount`。

试跑洞察示意：

<p align="center">
  <img src="img/链路试跑RAGDemo.png" alt="RAG 试跑" width="720" />
</p>

可选配置：

```yaml
liteflow:
  langchain:
    rag:
      max-results: 3
      min-score: 0.45
```

## 与 Demo7（LiteFlow Re-Act）对比

| 维度 | Demo7 | Demo8 | Demo9 | Demo10 |
|------|-------|-------|-------|--------|
| 框架 | liteflow-react-agent | LangChain4j | LangGraph4j | LangChain4j RAG |
| 形态 | ReActAgentComponent | AiServices + Tool | StateGraph 条件边 | Embedding 检索 + Chat |
| 典型用途 | LiteFlow 官方 Agent | Chat + Tool 混编 | 多步/分支 Agent 图 | 知识库增强问答 |
| Key/配额 | 共用模型配置与配额 | 同左 | 同左 | Chat 共用 Key；Embedding 本地 |

## 关键类

- `Lc4jChatModelFactory`：组装 OpenAI 兼容 `ChatModel`（DeepSeek）
- `RiskAssistant` + `RiskContextTool`：AiServices / Tool
- `RiskGraphBuilder` + `RiskGraphState`：LangGraph4j 图定义
- `RagKnowledgeBase` + `Lc4jRagComponent`：本地 Embedding + 内存向量 RAG

## 扩展建议（后续）

1. 知识库改为可上传文件 / 落盘持久化（替换 InMemory）
2. Graph Checkpoint：`MemorySaver` / Redis saver，支持中断恢复
3. 多 Agent：在 LangGraph 内加协作节点，外层仍用 LiteFlow 接业务落库
