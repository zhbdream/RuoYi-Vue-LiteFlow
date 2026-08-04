-- ----------------------------
-- Phase 8B：知识库 + 菜单 + Demo 链路
-- 依赖：先执行 phase8_ai_kit_platform.sql
-- ----------------------------

CREATE TABLE IF NOT EXISTS `ai_knowledge_base` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_code`     varchar(64)  NOT NULL COMMENT '知识库编码',
  `kb_name`     varchar(128) DEFAULT NULL COMMENT '名称',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  `chunk_count` int          DEFAULT 0 COMMENT '分片数',
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT '',
  `update_time` datetime     DEFAULT NULL,
  `remark`      varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_kb_code` (`kb_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 知识库';

CREATE TABLE IF NOT EXISTS `ai_knowledge_doc` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id`         bigint       NOT NULL COMMENT '知识库ID',
  `doc_name`      varchar(256) NOT NULL COMMENT '文档名',
  `file_path`     varchar(512) DEFAULT NULL COMMENT '存储路径',
  `content_text`  mediumtext   COMMENT '原文（txt/md）',
  `status`        char(1)      NOT NULL DEFAULT '0' COMMENT '0待索引 1已索引 2失败',
  `chunk_count`   int          DEFAULT 0,
  `create_by`     varchar(64)  DEFAULT '',
  `create_time`   datetime     DEFAULT NULL,
  `update_by`     varchar(64)  DEFAULT '',
  `update_time`   datetime     DEFAULT NULL,
  `remark`        varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_doc_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 知识库文档';

CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
  `id`          bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id`       bigint     NOT NULL COMMENT '知识库ID',
  `doc_id`      bigint     NOT NULL COMMENT '文档ID',
  `chunk_index` int        NOT NULL DEFAULT 0 COMMENT '分片序号',
  `content`     text       NOT NULL COMMENT '分片文本',
  `create_time` datetime   DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_chunk_kb` (`kb_id`),
  KEY `idx_ai_chunk_doc` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 知识库分片';

-- 种子知识库（售后政策，与 classpath:kb 对齐的示例文）
INSERT INTO `ai_knowledge_base` (`kb_code`, `kb_name`, `description`, `status`, `chunk_count`, `create_by`, `create_time`, `remark`)
SELECT 'after-sales', '售后政策', '退货/换货/物流示例知识库', '0', 0, 'system', NOW(), 'Phase B 种子'
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `kb_code` = 'after-sales');

INSERT INTO `ai_knowledge_doc` (`kb_id`, `doc_name`, `content_text`, `status`, `chunk_count`, `create_by`, `create_time`, `remark`)
SELECT kb.id, 'after-sales-return.md',
'## 退货政策
1. 签收后 7 天内，商品未使用且包装完好，可申请无理由退货。
2. 食品、贴身衣物、定制商品不支持无理由退货。
3. 退货运费：质量问题商家承担；无理由退货买家承担。
4. 退款在仓库验收通过后 3 个工作日内原路退回。',
'0', 0, 'system', NOW(), '种子文档'
FROM `ai_knowledge_base` kb
WHERE kb.kb_code = 'after-sales'
  AND NOT EXISTS (SELECT 1 FROM `ai_knowledge_doc` d WHERE d.kb_id = kb.id AND d.doc_name = 'after-sales-return.md');

INSERT INTO `ai_knowledge_doc` (`kb_id`, `doc_name`, `content_text`, `status`, `chunk_count`, `create_by`, `create_time`, `remark`)
SELECT kb.id, 'after-sales-exchange.md',
'## 换货政策
1. 商品存在质量问题或错发，签收后 15 天内可换货。
2. 换货需保持原包装与配件齐全。
3. 同价换货免运费；补差价换货按差价支付。',
'0', 0, 'system', NOW(), '种子文档'
FROM `ai_knowledge_base` kb
WHERE kb.kb_code = 'after-sales'
  AND NOT EXISTS (SELECT 1 FROM `ai_knowledge_doc` d WHERE d.kb_id = kb.id AND d.doc_name = 'after-sales-exchange.md');

INSERT INTO `ai_knowledge_doc` (`kb_id`, `doc_name`, `content_text`, `status`, `chunk_count`, `create_by`, `create_time`, `remark`)
SELECT kb.id, 'after-sales-shipping.md',
'## 物流时效
1. 现货订单 48 小时内发货。
2. 偏远地区可能延迟 2-3 天。
3. 物流异常可在订单页申请催促或改址（未出库前）。',
'0', 0, 'system', NOW(), '种子文档'
FROM `ai_knowledge_base` kb
WHERE kb.kb_code = 'after-sales'
  AND NOT EXISTS (SELECT 1 FROM `ai_knowledge_doc` d WHERE d.kb_id = kb.id AND d.doc_name = 'after-sales-shipping.md');

-- 绑定 rag 智能体到售后知识库
INSERT INTO `ai_agent_knowledge` (`agent_id`, `kb_id`, `sort`)
SELECT a.id, kb.id, 0
FROM `ai_agent` a
JOIN `ai_knowledge_base` kb ON kb.kb_code = 'after-sales'
WHERE a.agent_code = 'rag'
  AND NOT EXISTS (SELECT 1 FROM `ai_agent_knowledge` x WHERE x.agent_id = a.id AND x.kb_id = kb.id);

UPDATE `ai_agent`
SET `system_prompt` = '你是知识问答助手。请优先依据【参考资料】回答售后/政策类问题；资料不足时明确说明，使用中文。'
WHERE `agent_code` = 'rag'
  AND (`system_prompt` IS NULL OR `system_prompt` LIKE '你是知识问答助手%');

-- 菜单：知识库管理
INSERT INTO `sys_menu`
SELECT 2304, '知识库管理', 2300, 4, 'knowledge', 'aikit/knowledge/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:knowledge:list', 'documentation', 'admin', NOW(), '', NULL, 'AI Kit 知识库'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2304);

INSERT INTO `sys_menu` SELECT 2340, '知识库查询', 2304, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2340);
INSERT INTO `sys_menu` SELECT 2341, '知识库新增', 2304, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2341);
INSERT INTO `sys_menu` SELECT 2342, '知识库修改', 2304, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2342);
INSERT INTO `sys_menu` SELECT 2343, '知识库删除', 2304, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2343);
INSERT INTO `sys_menu` SELECT 2344, '文档上传', 2304, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:upload', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2344);
INSERT INTO `sys_menu` SELECT 2345, '重建索引', 2304, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:knowledge:reindex', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2345);

INSERT INTO `sys_role_menu` SELECT 2, m.menu_id FROM (
  SELECT 2304 AS menu_id UNION SELECT 2340 UNION SELECT 2341 UNION SELECT 2342
  UNION SELECT 2343 UNION SELECT 2344 UNION SELECT 2345
) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);

-- LiteFlow Demo：调用 AI Kit Agent（agentCode=rag）
INSERT INTO `lf_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `graph_json`, `enable`, `status`, `draft_flag`, `version`, `context_class`, `route_el`, `namespace`, `webhook_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 'ruoyi-liteflow', 'aiKitAgentDemo', 'Demo11 AI Kit 配置驱动 Agent',
       'THEN(aiKitAgentPrepare, aiKitAgent);', NULL, 1, '0', '0', 1,
       'com.ruoyiliteflow.langchain.domain.AiKitAgentContext', NULL, NULL, NULL,
       'admin', NOW(), '', NULL, 'Phase B 薄适配'
WHERE NOT EXISTS (SELECT 1 FROM `lf_chain` WHERE `application_name` = 'ruoyi-liteflow' AND `chain_name` = 'aiKitAgentDemo');
