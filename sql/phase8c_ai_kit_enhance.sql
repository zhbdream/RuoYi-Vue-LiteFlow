-- ----------------------------
-- Phase 8C：Skills / 记忆 / 上下文 + MCP 动态工具支撑
-- 依赖：phase8 + phase8b
-- ----------------------------

CREATE TABLE IF NOT EXISTS `ai_skill` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `skill_code`  varchar(64)  NOT NULL COMMENT '技能编码',
  `skill_name`  varchar(128) DEFAULT NULL,
  `skill_type`  varchar(16)  NOT NULL DEFAULT 'prompt' COMMENT 'prompt|http',
  `content`     mediumtext   COMMENT 'prompt 模板或 HTTP URL',
  `description` varchar(512) DEFAULT NULL,
  `enabled`     char(1)      NOT NULL DEFAULT '1',
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT '',
  `update_time` datetime     DEFAULT NULL,
  `remark`      varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_skill_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 技能';

CREATE TABLE IF NOT EXISTS `ai_agent_skill` (
  `agent_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `sort`     int DEFAULT 0,
  PRIMARY KEY (`agent_id`, `skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体-技能关联';

CREATE TABLE IF NOT EXISTS `ai_memory_item` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `agent_code`  varchar(64)  NOT NULL,
  `session_id`  varchar(64)  NOT NULL DEFAULT 'default',
  `principal`   varchar(64)  DEFAULT 'anonymous',
  `memory_type` varchar(16)  NOT NULL DEFAULT 'turn' COMMENT 'turn|summary|fact',
  `role`        varchar(16)  DEFAULT NULL COMMENT 'user|assistant|system',
  `content`     mediumtext   NOT NULL,
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_mem_agent_session` (`agent_code`, `session_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 记忆条目';

CREATE TABLE IF NOT EXISTS `ai_context_policy` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT,
  `policy_code`        varchar(64)  NOT NULL,
  `policy_name`        varchar(128) DEFAULT NULL,
  `window_size`        int          NOT NULL DEFAULT 10 COMMENT '记忆窗口条数',
  `enable_summary`     char(1)      NOT NULL DEFAULT '0' COMMENT '超窗是否写摘要',
  `variable_template`  varchar(512) DEFAULT NULL COMMENT '变量模板，如 用户={{principal}}',
  `is_default`         char(1)      NOT NULL DEFAULT '0',
  `enabled`            char(1)      NOT NULL DEFAULT '1',
  `create_by`          varchar(64)  DEFAULT '',
  `create_time`        datetime     DEFAULT NULL,
  `update_by`          varchar(64)  DEFAULT '',
  `update_time`        datetime     DEFAULT NULL,
  `remark`             varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_ctx_policy` (`policy_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Kit 上下文策略';

-- ai_agent 增加上下文策略绑定
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent' AND COLUMN_NAME = 'context_policy_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `ai_agent` ADD COLUMN `context_policy_id` bigint DEFAULT NULL COMMENT ''上下文策略'' AFTER `temperature`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 种子：默认上下文策略
INSERT INTO `ai_context_policy` (`policy_code`, `policy_name`, `window_size`, `enable_summary`, `variable_template`, `is_default`, `enabled`, `create_by`, `create_time`, `remark`)
SELECT 'default', '默认策略', 8, '1', '调用方={{principal}}', '1', '1', 'system', NOW(), 'Phase C'
WHERE NOT EXISTS (SELECT 1 FROM `ai_context_policy` WHERE `policy_code` = 'default');

-- 种子技能
INSERT INTO `ai_skill` (`skill_code`, `skill_name`, `skill_type`, `content`, `description`, `enabled`, `create_by`, `create_time`, `remark`)
SELECT 'concise-zh', '简洁中文', 'prompt',
       '回答尽量简洁，使用中文，避免空话。', '通用风格技能', '1', 'system', NOW(), 'Phase C'
WHERE NOT EXISTS (SELECT 1 FROM `ai_skill` WHERE `skill_code` = 'concise-zh');

INSERT INTO `ai_skill` (`skill_code`, `skill_name`, `skill_type`, `content`, `description`, `enabled`, `create_by`, `create_time`, `remark`)
SELECT 'cite-kb', '引用资料', 'prompt',
       '若存在【参考资料】或【历史记忆】，回答中简要引用关键点。', 'RAG/记忆增强', '1', 'system', NOW(), 'Phase C'
WHERE NOT EXISTS (SELECT 1 FROM `ai_skill` WHERE `skill_code` = 'cite-kb');

-- chat 绑定简洁技能
INSERT INTO `ai_agent_skill` (`agent_id`, `skill_id`, `sort`)
SELECT a.id, s.id, 0 FROM `ai_agent` a JOIN `ai_skill` s ON s.skill_code = 'concise-zh'
WHERE a.agent_code = 'chat'
  AND NOT EXISTS (SELECT 1 FROM `ai_agent_skill` x WHERE x.agent_id = a.id AND x.skill_id = s.id);

INSERT INTO `ai_agent_skill` (`agent_id`, `skill_id`, `sort`)
SELECT a.id, s.id, 0 FROM `ai_agent` a JOIN `ai_skill` s ON s.skill_code = 'cite-kb'
WHERE a.agent_code = 'rag'
  AND NOT EXISTS (SELECT 1 FROM `ai_agent_skill` x WHERE x.agent_id = a.id AND x.skill_id = s.id);

-- 默认策略绑到 chat/rag（若列已存在）
UPDATE `ai_agent` a
JOIN `ai_context_policy` p ON p.policy_code = 'default'
SET a.context_policy_id = p.id
WHERE a.agent_code IN ('chat', 'rag') AND (a.context_policy_id IS NULL OR a.context_policy_id = 0);

-- 动态 MCP 示例工具（元数据；由 mcp dynamic registry 暴露）
INSERT INTO `ai_tool` (`tool_code`, `tool_name`, `tool_type`, `description`, `invoke_key`, `mcp_server_key`, `enabled`, `create_by`, `create_time`, `remark`)
SELECT 'echo_ping', '回声探测', 'mcp', '动态注册示例：原样回显参数', 'echo', 'ai-core', '1', 'system', NOW(), 'Phase C dynamic'
WHERE NOT EXISTS (SELECT 1 FROM `ai_tool` WHERE `tool_code` = 'echo_ping');

-- 菜单
INSERT INTO `sys_menu`
SELECT 2305, '技能管理', 2300, 5, 'skill', 'aikit/skill/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:skill:list', 'education', 'admin', NOW(), '', NULL, 'AI Kit Skills'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2305);

INSERT INTO `sys_menu`
SELECT 2306, '记忆管理', 2300, 6, 'memory', 'aikit/memory/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:memory:list', 'redis', 'admin', NOW(), '', NULL, 'AI Kit Memory'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2306);

INSERT INTO `sys_menu`
SELECT 2307, '上下文策略', 2300, 7, 'context', 'aikit/context/index', '', '', 1, 0, 'C', '0', '0',
       'aikit:context:list', 'dict', 'admin', NOW(), '', NULL, 'AI Kit Context'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2307);

INSERT INTO `sys_menu` SELECT 2350, '技能查询', 2305, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:skill:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2350);
INSERT INTO `sys_menu` SELECT 2351, '技能新增', 2305, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:skill:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2351);
INSERT INTO `sys_menu` SELECT 2352, '技能修改', 2305, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:skill:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2352);
INSERT INTO `sys_menu` SELECT 2353, '技能删除', 2305, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:skill:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2353);

INSERT INTO `sys_menu` SELECT 2360, '记忆查询', 2306, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:memory:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2360);
INSERT INTO `sys_menu` SELECT 2361, '记忆新增', 2306, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:memory:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2361);
INSERT INTO `sys_menu` SELECT 2362, '记忆删除', 2306, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:memory:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2362);

INSERT INTO `sys_menu` SELECT 2370, '策略查询', 2307, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:context:query', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2370);
INSERT INTO `sys_menu` SELECT 2371, '策略新增', 2307, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:context:add', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2371);
INSERT INTO `sys_menu` SELECT 2372, '策略修改', 2307, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:context:edit', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2372);
INSERT INTO `sys_menu` SELECT 2373, '策略删除', 2307, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aikit:context:remove', '#', 'admin', NOW(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2373);

INSERT INTO `sys_role_menu` SELECT 2, m.menu_id FROM (
  SELECT 2305 AS menu_id UNION SELECT 2306 UNION SELECT 2307
  UNION SELECT 2350 UNION SELECT 2351 UNION SELECT 2352 UNION SELECT 2353
  UNION SELECT 2360 UNION SELECT 2361 UNION SELECT 2362
  UNION SELECT 2370 UNION SELECT 2371 UNION SELECT 2372 UNION SELECT 2373
) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);
