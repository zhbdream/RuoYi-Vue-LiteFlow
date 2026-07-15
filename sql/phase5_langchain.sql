-- Phase5: LangChain4j / LangGraph4j Demo 链路（已有库增量执行）
-- 依赖：模型配置页默认 Key，或环境变量 DEEPSEEK_API_KEY

INSERT INTO `lf_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `graph_json`, `enable`, `status`, `draft_flag`, `version`, `context_class`, `route_el`, `namespace`, `webhook_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 'ruoyi-liteflow', 'lc4jChatDemo', 'Demo8 LangChain4j Chat+Tool 风控', 'THEN(lc4jPrepare, lc4jChat, lc4jNotify);', NULL, 1, '0', '0', 1,
       'com.ruoyiliteflow.langchain.domain.Lc4jRiskContext', NULL, NULL, NULL, 'admin', NOW(), '', NULL, 'Phase5 LangChain4j'
WHERE NOT EXISTS (SELECT 1 FROM `lf_chain` WHERE `application_name` = 'ruoyi-liteflow' AND `chain_name` = 'lc4jChatDemo');

INSERT INTO `lf_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `graph_json`, `enable`, `status`, `draft_flag`, `version`, `context_class`, `route_el`, `namespace`, `webhook_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 'ruoyi-liteflow', 'lc4jGraphDemo', 'Demo9 LangGraph4j 状态图风控', 'THEN(lc4jPrepare, lc4jGraph, lc4jNotify);', NULL, 1, '0', '0', 1,
       'com.ruoyiliteflow.langchain.domain.Lc4jRiskContext', NULL, NULL, NULL, 'admin', NOW(), '', NULL, 'Phase5 LangGraph4j'
WHERE NOT EXISTS (SELECT 1 FROM `lf_chain` WHERE `application_name` = 'ruoyi-liteflow' AND `chain_name` = 'lc4jGraphDemo');
