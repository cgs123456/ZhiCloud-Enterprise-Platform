-- ============================================================
-- V203：ERP 固定资产处置/报废按钮权限播种
-- ============================================================
-- 背景（台账 §12.237）：固定资产新增「闲置/启用/处置/报废」四个状态流转端点；
-- 其中闲置/启用复用 erp:fixed-asset:update，处置/报废为终态动作，使用独立权限
-- erp:fixed-asset:dispose。本迁移把该权限串种进 system_menu（V186 已种
-- fixed-asset 目录 9373 与 query/create/update/delete 按钮 9374-9377）。
--
-- id 选用 9605（9300-9500 已满，9605-9699 经全仓检索为空闲段）。
-- 刻意不写 system_role_menu：超级管理员走角色旁路即可见（与 V134 口径一致）。
-- ============================================================

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9605 AS `id`, '固定资产处置/报废' AS `name`, 'erp:fixed-asset:dispose' AS `permission`, 3 AS `type`, 5 AS `sort`, 9373 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ==================== 自检 ====================
SELECT CASE WHEN COUNT(*) = 1 THEN 'V203_DISPOSE_PERM PASS'
            ELSE CONCAT('V203_DISPOSE_PERM FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` = 'erp:fixed-asset:dispose';

SELECT CASE WHEN COUNT(*) = 0 THEN 'V203_NO_ORPHAN PASS'
            ELSE CONCAT('V203_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` c
 WHERE c.`deleted` = b'0' AND c.`id` = 9605
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p
                    WHERE p.`id` = c.`parent_id` AND p.`deleted` = b'0');
