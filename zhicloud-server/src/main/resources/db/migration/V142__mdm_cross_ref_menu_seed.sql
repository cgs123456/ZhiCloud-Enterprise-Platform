-- ============================================================
-- V142：主数据跨域对照管理 API 的菜单/权限种子（R-014）
-- ============================================================
-- MdmCrossRefController 用了三个新权限点：
--   system:mdm-cross-ref:query   分页查询本租户映射
--   system:mdm-cross-ref:create  登记/更新（upsert）一条映射
--   system:mdm-cross-ref:delete  失效一条映射
-- 凭空造权限串会因无 system_menu 行而对普通角色恒 403（§12.95.3 的教训），
-- 所以这三行权限点必须与 Controller 同批落地，否则接口交上去也用不了。
--
-- 列集合按 information_schema 核对过：system_menu 没有 tenant_id（全局表，
-- 租户可见性由角色决定）；菜单挂在已有的「系统管理」目录（id=1, type=1）下。
-- 幂等：菜单/按钮按 (type, permission) 预检，且只认 deleted=b'0' 的行
-- （与 V134 一致）。刻意不写 system_role_menu：超级管理员走角色旁路即可见，
-- 给普通角色授权是运营动作，不在种子里伪造。
-- ============================================================

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 7900 AS `id`, '主数据跨域对照' AS `name`, 'system:mdm-cross-ref:query' AS `permission`, 2 AS `type`, 60 AS `sort`, 1 AS `parent_id`, 'mdm-cross-ref' AS `path`, 'ep:connection' AS `icon`, 'system/mdmcrossref/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 7901, '主数据对照查询', 'system:mdm-cross-ref:query', 3, 1, 7900, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7902, '主数据对照登记', 'system:mdm-cross-ref:create', 3, 2, 7900, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7903, '主数据对照失效', 'system:mdm-cross-ref:delete', 3, 3, 7900, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` e
     WHERE e.`deleted` = b'0'
       AND e.`type` = t.`type` AND e.`permission` = t.`permission` AND t.`permission` <> ''
);

-- 自检 1：三个权限点都在（缺一即接口用不了）
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission IN ('system:mdm-cross-ref:query','system:mdm-cross-ref:create','system:mdm-cross-ref:delete')) = 3
            THEN 'V142_MDM_MENUS_SEEDED PASS'
            ELSE 'V142_MDM_MENUS_SEEDED FAIL 权限点不齐，接口会恒 403' END AS check_result;

-- 自检 2：菜单没挂成孤儿（父必须是已存在的行）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V142_NO_ORPHAN PASS'
            ELSE CONCAT('V142_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.deleted = b'0' AND m.parent_id <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.id = m.parent_id AND p.deleted = b'0');
