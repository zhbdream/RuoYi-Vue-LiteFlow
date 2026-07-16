# RuoYi-Vue-LiteFlow 文档索引

> 本地文档入口。独立静态站点 / 在线 Demo 环境可后续单独部署。

| 文档 | 说明 |
|------|------|
| [../README.md](../README.md) | 项目简介、Quick Start、Demo 矩阵 |
| [EDITOR.md](EDITOR.md) | 可视化编排器使用指南 |
| [API.md](API.md) | 开放 API / 内部执行 / Webhook / 定时任务 |
| [AGENT.md](AGENT.md) | Re-Act Agent（DeepSeek）配置与 Demo7 |
| [LANGCHAIN.md](LANGCHAIN.md) | LangChain4j / LangGraph4j / RAG（Demo8/9/10） |
| [CHAT.md](CHAT.md) | 内部 AI 助手（多轮对话 + SSE） |
| [demo/README.md](demo/README.md) | Demo 请求样例 |
| [img/README.md](img/README.md) | 界面截图素材索引 |

## Demo 快速跳转

| chainId | 能力 |
|---------|------|
| helloChain | THEN 入门 |
| orderProcess | IF / SWITCH |
| dynamicPricing | 脚本组件 |
| parallelAudit | WHEN |
| resilientNotify | CATCH / RETRY |
| batchProcess | FOR |
| routeDemo | 决策路由 |
| fallbackDemo | 声明式 + `@FallbackCmp` 降级 |
| agentRiskDemo | DeepSeek Re-Act Agent 风控 |
| lc4jChatDemo | LangChain4j Chat + Tool |
| lc4jGraphDemo | LangGraph4j 状态图风控 |
| lc4jRagDemo | LangChain4j RAG 售后问答 |

## AI 入口（非链路）

| 菜单 | 说明 |
|------|------|
| 模型配置 | AES 加密入库的 OpenAI 兼容 Key（DeepSeek 等） |
| AI助手 | 后台内部多轮对话，复用默认模型与配额 |
