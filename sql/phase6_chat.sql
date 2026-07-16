-- Phase6: 内部 AI 助手（轻量多轮对话）
-- 已有库增量执行；全新安装请用含下列对象的 ry-vue.sql

-- ----------------------------
-- Tables
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lf_chat_session` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       varchar(128) DEFAULT NULL COMMENT '会话标题',
  `user_name`   varchar(64)  NOT NULL COMMENT '归属用户',
  `model_code`  varchar(64)  DEFAULT NULL COMMENT '模型标识',
  `model_name`  varchar(128) DEFAULT NULL COMMENT '模型名',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '0正常 1删除',
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT '',
  `update_time` datetime     DEFAULT NULL,
  `remark`      varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_user` (`user_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内部助手会话';

CREATE TABLE IF NOT EXISTS `lf_chat_message` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  bigint       NOT NULL COMMENT '会话ID',
  `role`        varchar(16)  NOT NULL COMMENT 'user/assistant/system',
  `content`     mediumtext   NOT NULL COMMENT '消息内容',
  `token_count` int          DEFAULT NULL COMMENT 'Token 用量',
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内部助手消息';

-- ----------------------------
-- Menus（LiteFlow编排 → AI助手）
-- ----------------------------
INSERT INTO `sys_menu`
SELECT 2010, 'AI助手', 2000, 10, 'chat', 'liteflow/chat/index', '', '', 1, 0, 'C', '0', '0',
       'liteflow:chat:list', 'message', 'admin', NOW(), '', NULL, '内部 AI 对话助手'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2010);

INSERT INTO `sys_menu`
SELECT 2270, '会话查询', 2010, 1, '', '', '', '', 1, 0, 'F', '0', '0',
       'liteflow:chat:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2270);

INSERT INTO `sys_menu`
SELECT 2271, '会话发送', 2010, 2, '', '', '', '', 1, 0, 'F', '0', '0',
       'liteflow:chat:send', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2271);

INSERT INTO `sys_menu`
SELECT 2272, '会话删除', 2010, 3, '', '', '', '', 1, 0, 'F', '0', '0',
       'liteflow:chat:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2272);

-- 普通角色（role_id=2）授权
INSERT INTO `sys_role_menu` SELECT 2, 2010 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 2010);
INSERT INTO `sys_role_menu` SELECT 2, 2270 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 2270);
INSERT INTO `sys_role_menu` SELECT 2, 2271 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 2271);
INSERT INTO `sys_role_menu` SELECT 2, 2272 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 2272);
