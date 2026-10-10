-- V158: FMS 财务管理系统 config 域菜单与按钮权限种子（8 个菜单 + 按钮）
-- 写法沿用 V134 口径：目录 type=1、parent_id=0、path=/fms；菜单 type=2、component 指向前端实际页面；
-- 按钮 type=3、permission 与代码里实际声明的 @PreAuthorize 一一对应（非手抄清单）。
-- 权限串取自前端视图 v-hasPermi 与后端 @PreAuthorize 双侧交集（导出/导入用 export/import）。
-- 本轮只种「后端已实现」的 8 个配置菜单；科目/初始余额/凭证/账簿/结账/报表待后端完成后追加。
-- system_menu 无 tenant_id（全局表）；幂等：目录按 (path,type=1)、菜单/按钮按 (permission,type) 预检，只认 deleted = b'0'。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9000 AS `id`, '财务管理 FMS' AS `name`, '' AS `permission`, 1 AS `type`, 1 AS `sort`, 0 AS `parent_id`, '/fms' AS `path`, 'ep:wallet' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9010, '账套管理', 'fms:config:account-set:query', 2, 1, 9000, 'config/account-set', 'ep:notebook', 'fms/config/account-set/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9011, '账套新增', 'fms:config:account-set:create', 3, 1, 9010, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9012, '账套修改', 'fms:config:account-set:update', 3, 2, 9010, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9013, '账套初始化', 'fms:config:account-set:initialize', 3, 3, 9010, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9014, '账套成员授权', 'fms:config:account-set:authorize', 3, 4, 9010, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9020, '币别管理', 'fms:config:currency:query', 2, 2, 9000, 'config/currency', 'ep:coin', 'fms/config/currency/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9021, '币别新增', 'fms:config:currency:create', 3, 1, 9020, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9022, '币别修改', 'fms:config:currency:update', 3, 2, 9020, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9023, '币别删除', 'fms:config:currency:delete', 3, 3, 9020, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9030, '凭证字管理', 'fms:config:voucher-word:query', 2, 3, 9000, 'config/voucher-word', 'ep:document', 'fms/config/voucher-word/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9031, '凭证字新增', 'fms:config:voucher-word:create', 3, 1, 9030, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9032, '凭证字修改', 'fms:config:voucher-word:update', 3, 2, 9030, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9033, '凭证字删除', 'fms:config:voucher-word:delete', 3, 3, 9030, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9040, '常用摘要管理', 'fms:config:digest:query', 2, 4, 9000, 'config/digest', 'ep:tickets', 'fms/config/digest/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9041, '摘要新增', 'fms:config:digest:create', 3, 1, 9040, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9042, '摘要修改', 'fms:config:digest:update', 3, 2, 9040, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9043, '摘要删除', 'fms:config:digest:delete', 3, 3, 9040, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9050, '辅助核算管理', 'fms:config:auxiliary:query', 2, 5, 9000, 'config/auxiliary', 'ep:grid', 'fms/config/auxiliary/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9051, '辅助核算新增', 'fms:config:auxiliary:create', 3, 1, 9050, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9052, '辅助核算修改', 'fms:config:auxiliary:update', 3, 2, 9050, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9053, '辅助核算删除', 'fms:config:auxiliary:delete', 3, 3, 9050, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9054, '辅助核算导出', 'fms:config:auxiliary:export', 3, 4, 9050, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9055, '辅助核算导入', 'fms:config:auxiliary:import', 3, 5, 9050, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9060, '凭证模板管理', 'fms:config:voucher-template:query', 2, 6, 9000, 'config/voucher-template', 'ep:copy-document', 'fms/config/voucher-template/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9061, '凭证模板新增', 'fms:config:voucher-template:create', 3, 1, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9062, '凭证模板修改', 'fms:config:voucher-template:update', 3, 2, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9063, '凭证模板删除', 'fms:config:voucher-template:delete', 3, 3, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9064, '模板分类新增', 'fms:config:voucher-template-category:create', 3, 4, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9065, '模板分类修改', 'fms:config:voucher-template-category:update', 3, 5, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9066, '模板分类删除', 'fms:config:voucher-template-category:delete', 3, 6, 9060, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9070, '财务指标管理', 'fms:config:finance-indicator:query', 2, 7, 9000, 'config/finance-indicator', 'ep:data-line', 'fms/config/finance-indicator/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9071, '指标新增', 'fms:config:finance-indicator:create', 3, 1, 9070, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9072, '指标修改', 'fms:config:finance-indicator:update', 3, 2, 9070, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9073, '指标删除', 'fms:config:finance-indicator:delete', 3, 3, 9070, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9080, '账套参数', 'fms:config:finance-parameter:update', 2, 8, 9000, 'config/finance-parameter', 'ep:setting', 'fms/config/finance-parameter/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` = 1 AND m.`path` = t.`path` AND m.`type` = 1 AND m.`parent_id` = 0)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);
