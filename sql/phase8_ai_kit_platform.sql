-- ----------------------------
-- Phase 8 AI Kit Platform（模型 / 工具 / 智能体）
-- 执行前请确认库名；可与 ry-vue 同库
-- ----------------------------

DROP TABLE IF EXISTS `ai_agent_knowledge`;
DROP TABLE IF EXISTS `ai_agent_tool`;
DROP TABLE IF EXISTS `ai_agent`;
DROP TABLE IF EXISTS `ai_tool`;
DROP TABLE IF EXISTS `ai_model`;

CREATE TABLE `ai_model` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_code`        varchar(64)  NOT NULL COMMENT '模型编码',
  `model_name`        varchar(128) DEFAULT NULL COMMENT '显示名称',
  `provider`          varchar(32)  NOT NULL DEFAULT 'deepseek' COMMENT '供应商',
  `base_url`          varchar(512) DEFAULT NULL COMMENT 'API Base URL',
  `model`             varchar(128) NOT NULL DEFAULT 'deepseek-chat' COMMENT '模型名',
  `api_key_enc`       varchar(512) DEFAULT NULL COMMENT 'AES 加密 API Key',
  `status`            char(1)      NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  `is_default`        char(1)      NOT NULL DEFAULT '0' COMMENT '是否默认',
  `daily_call_limit`  int          DEFAULT NULL COMMENT '单用户日调用上限',
  `daily_token_limit` int          DEFAULT NULL COMMENT '单用户日 Token 上限',
  `create_by`         varchar(64)  DEFAULT '',
  `create_time`       datetime     DEFAULT NULL,
  `update_by`         varchar(64)  DEFAULT '',
  `update_time`       datetime     DEFAULT NULL,
  `remark`            varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_code` (`model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 模型配置';

CREATE TABLE `ai_tool` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tool_code`         varchar(64)  NOT NULL COMMENT '工具编码',
  `tool_name`         varchar(128) DEFAULT NULL COMMENT '工具名称',
  `tool_type`         varchar(16)  NOT NULL DEFAULT 'local' COMMENT 'local|mcp',
  `description`       varchar(512) DEFAULT NULL COMMENT '描述',
  `input_schema_json` text         COMMENT 'JSON Schema',
  `invoke_key`        varchar(256) DEFAULT NULL COMMENT 'local=bean/方法键；mcp=tool 名',
  `mcp_server_key`    varchar(64)  DEFAULT NULL COMMENT '如 ai-core',
  `enabled`           char(1)      NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `create_by`         varchar(64)  DEFAULT '',
  `create_time`       datetime     DEFAULT NULL,
  `update_by`         varchar(64)  DEFAULT '',
  `update_time`       datetime     DEFAULT NULL,
  `remark`            varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 工具登记';

CREATE TABLE `ai_agent` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_code`     varchar(64)   NOT NULL COMMENT '智能体编码',
  `agent_name`     varchar(128)  DEFAULT NULL COMMENT '名称',
  `system_prompt`  text          COMMENT '系统提示词',
  `model_id`       bigint        DEFAULT NULL COMMENT '绑定模型 ai_model.id',
  `temperature`    decimal(3,2)  DEFAULT 0.30 COMMENT '温度',
  `enabled`        char(1)       NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `create_by`      varchar(64)   DEFAULT '',
  `create_time`    datetime      DEFAULT NULL,
  `update_by`      varchar(64)   DEFAULT '',
  `update_time`    datetime      DEFAULT NULL,
  `remark`         varchar(500)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_code` (`agent_code`),
  KEY `idx_ai_agent_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 智能体';

CREATE TABLE `ai_agent_tool` (
  `agent_id` bigint NOT NULL,
  `tool_id`  bigint NOT NULL,
  `sort`     int    DEFAULT 0,
  PRIMARY KEY (`agent_id`, `tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体-工具关联';

CREATE TABLE `ai_agent_knowledge` (
  `agent_id` bigint NOT NULL,
  `kb_id`    bigint NOT NULL,
  `sort`     int    DEFAULT 0,
  PRIMARY KEY (`agent_id`, `kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体-知识库关联（Phase B）';

-- 种子工具（元数据登记，Phase A 运行仍可走代码内逻辑）
INSERT INTO `ai_tool` (`tool_code`, `tool_name`, `tool_type`, `description`, `invoke_key`, `mcp_server_key`, `enabled`, `create_by`, `create_time`, `remark`)
VALUES
('chat_completion', '对话补全', 'mcp', 'MCP chat_completion', 'chat_completion', 'ai-core', '1', 'system', sysdate(), 'Phase A 静态 MCP'),
('risk_analyze', '风控分析', 'mcp', 'MCP risk_analyze', 'risk_analyze', 'ai-core', '1', 'system', sysdate(), NULL),
('rag_ask', '知识问答', 'mcp', 'MCP rag_ask', 'rag_ask', 'ai-core', '1', 'system', sysdate(), NULL),
('list_chains', '链路列表', 'mcp', '治理 Demo list_chains', 'list_chains', 'lf-governance', '1', 'system', sysdate(), NULL),
('dashboard_summary', '监控摘要', 'mcp', '治理 Demo dashboard_summary', 'dashboard_summary', 'lf-governance', '1', 'system', sysdate(), NULL);

-- 种子智能体（与 boot 现有 chat/risk/rag/ops 对齐；API Key 走 yml 或后续写入 ai_model）
INSERT INTO `ai_agent` (`agent_code`, `agent_name`, `system_prompt`, `model_id`, `temperature`, `enabled`, `create_by`, `create_time`, `remark`)
VALUES
('chat', '通用对话', '你是内部助手，回答简洁、准确，使用中文。', NULL, 0.30, '1', 'system', sysdate(), '种子'),
('risk', '风控分析', '你是风控分析助手。根据用户描述评估风险等级（低/中/高）并给出理由，使用中文。', NULL, 0.20, '1', 'system', sysdate(), '种子'),
('rag', '知识问答', '你是知识问答助手。基于已知政策与常识回答，不确定时明确说明，使用中文。', NULL, 0.20, '1', 'system', sysdate(), '种子'),
('ops', '运维助手', '你是编排中台运维助手。根据用户问题给出可操作的排查建议，不要编造不存在的链路，使用中文。', NULL, 0.30, '1', 'system', sysdate(), '种子');

INSERT INTO `ai_agent_tool` (`agent_id`, `tool_id`, `sort`)
SELECT a.id, t.id, 0 FROM `ai_agent` a JOIN `ai_tool` t ON t.tool_code = 'chat_completion' WHERE a.agent_code = 'chat';

INSERT INTO `ai_agent_tool` (`agent_id`, `tool_id`, `sort`)
SELECT a.id, t.id, 0 FROM `ai_agent` a JOIN `ai_tool` t ON t.tool_code = 'risk_analyze' WHERE a.agent_code = 'risk';

INSERT INTO `ai_agent_tool` (`agent_id`, `tool_id`, `sort`)
SELECT a.id, t.id, 0 FROM `ai_agent` a JOIN `ai_tool` t ON t.tool_code = 'rag_ask' WHERE a.agent_code = 'rag';

INSERT INTO `ai_agent_tool` (`agent_id`, `tool_id`, `sort`)
SELECT a.id, t.id, x.sort FROM `ai_agent` a
JOIN (
  SELECT 'list_chains' AS tool_code, 0 AS sort
  UNION ALL SELECT 'dashboard_summary', 1
  UNION ALL SELECT 'chat_completion', 2
) x ON 1=1
JOIN `ai_tool` t ON t.tool_code = x.tool_code
WHERE a.agent_code = 'ops';

-- ----------------------------
-- Menus：AI能力（模型 / 工具 / 智能体）
-- ----------------------------
INSERT INTO `sys_menu`
SELECT 2300, 'AI能力', 0, 6, 'aikit', NULL, '', '', 1, 0, 'M', '0', '0',
       '', 'guide', 'admin', NOW(), '', NULL, 'AI Kit 配置面'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2300);

INSERT INTO `sys_menu`
SELECT 2301, '模型管理', 2300, 1, 'model', 'aikit/model/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:model:list', 'skill', 'admin', NOW(), '', NULL, 'AI Kit 模型'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2301);

INSERT INTO `sys_menu`
SELECT 2302, '工具管理', 2300, 2, 'tool', 'aikit/tool/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:tool:list', 'tool', 'admin', NOW(), '', NULL, 'AI Kit 工具'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2302);

INSERT INTO `sys_menu`
SELECT 2303, '智能体管理', 2300, 3, 'agent', 'aikit/agent/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:agent:list', 'peoples', 'admin', NOW(), '', NULL, 'AI Kit 智能体'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2303);

INSERT INTO `sys_menu` SELECT 2310, '模型查询', 2301, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:model:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2310);
INSERT INTO `sys_menu` SELECT 2311, '模型新增', 2301, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:model:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2311);
INSERT INTO `sys_menu` SELECT 2312, '模型修改', 2301, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:model:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2312);
INSERT INTO `sys_menu` SELECT 2313, '模型删除', 2301, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:model:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2313);
INSERT INTO `sys_menu` SELECT 2314, '模型测试', 2301, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:model:test', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2314);

INSERT INTO `sys_menu` SELECT 2320, '工具查询', 2302, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:tool:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2320);
INSERT INTO `sys_menu` SELECT 2321, '工具新增', 2302, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:tool:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2321);
INSERT INTO `sys_menu` SELECT 2322, '工具修改', 2302, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:tool:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2322);
INSERT INTO `sys_menu` SELECT 2323, '工具删除', 2302, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:tool:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2323);

INSERT INTO `sys_menu` SELECT 2330, '智能体查询', 2303, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:agent:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2330);
INSERT INTO `sys_menu` SELECT 2331, '智能体新增', 2303, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:agent:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2331);
INSERT INTO `sys_menu` SELECT 2332, '智能体修改', 2303, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:agent:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2332);
INSERT INTO `sys_menu` SELECT 2333, '智能体删除', 2303, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:agent:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2333);
INSERT INTO `sys_menu` SELECT 2334, '智能体试跑', 2303, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:agent:run', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2334);

-- 普通角色授权
INSERT INTO `sys_role_menu` SELECT 2, m.menu_id FROM (
  SELECT 2300 AS menu_id UNION SELECT 2301 UNION SELECT 2302 UNION SELECT 2303
  UNION SELECT 2310 UNION SELECT 2311 UNION SELECT 2312 UNION SELECT 2313 UNION SELECT 2314
  UNION SELECT 2320 UNION SELECT 2321 UNION SELECT 2322 UNION SELECT 2323
  UNION SELECT 2330 UNION SELECT 2331 UNION SELECT 2332 UNION SELECT 2333 UNION SELECT 2334
) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);
