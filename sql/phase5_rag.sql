-- Phase5 RAG: LangChain4j 售后知识问答 Demo（已有库增量执行）
-- 依赖：模型配置页默认 Key 或 DEEPSEEK_API_KEY；本地 All-MiniLM Embedding（无额外向量库）

INSERT INTO `lf_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `graph_json`, `enable`, `status`, `draft_flag`, `version`, `context_class`, `route_el`, `namespace`, `webhook_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 'ruoyi-liteflow', 'lc4jRagDemo', 'Demo10 LangChain4j RAG 售后问答', 'THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify);', NULL, 1, '0', '0', 1,
       'com.ruoyiliteflow.langchain.domain.Lc4jRagContext', NULL, NULL, NULL, 'admin', NOW(), '', NULL, 'Phase5 LangChain4j RAG'
WHERE NOT EXISTS (SELECT 1 FROM `lf_chain` WHERE `application_name` = 'ruoyi-liteflow' AND `chain_name` = 'lc4jRagDemo');
