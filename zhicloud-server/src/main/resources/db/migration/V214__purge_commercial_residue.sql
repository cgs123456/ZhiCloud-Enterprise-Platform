-- ============================================================
-- V214：清理 V140 商业域移除后的最后一层存量残留
-- ============================================================
-- 背景（2026-10-20 实测既有库 zhicloud_sandbox_v140）：
--   1. V140 的字典清理只覆盖 pay/trade/member/product/promotion 前缀，
--      漏掉了同属商业域的 brokerage_*：7 个字典类型、28 条字典数据。
--      安装脚本已同步删除这 35 条种子，本迁移负责既有库。
--   2. system_tenant_package.menu_ids 仍保留 77 个 V140 已物理删除的菜单 id。
--      新建租户时 TenantServiceImpl#createTenantRole 会原样写入 system_role_menu，
--      而 PermissionServiceImpl#assignRoleMenu 不校验 menu_id 是否存在，所以先清套餐，
--      再清已建租户可能留下的孤儿角色菜单。
--   3. system_notify_message 仍有 2 条 brokerage_withdraw_audit_approve 站内信
--      （V1.1 基线种子重放；安装脚本已清理）。
--
-- 处理范围：
--   a. 删除商业前缀的站内信与站内信模板；
--   b. 删除商业前缀的字典数据/类型（brokerage 为重点，其余前缀重复执行无副作用）；
--   c. 按现存 system_menu 过滤 system_tenant_package.menu_ids，只改含悬空 id 的套餐；
--   d. 删除 system_role_menu 中指向不存在菜单的孤儿行。
--
-- 幂等：所有删除/过滤均按条件执行，重复执行安全；临时表先 DROP IF EXISTS。
-- 回滚：这些数据属于已下线的商业域，无需回滚；如需恢复只能从备份重放，
--       且重放后再次执行本迁移仍会清理。
-- ============================================================

-- 1. 站内信：模板与消息（消息有 tenant_id，但删除面只按已下线模板编码定位）
DELETE FROM `system_notify_message` WHERE SUBSTRING_INDEX(`template_code`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

DELETE FROM `system_notify_template` WHERE SUBSTRING_INDEX(`code`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

-- 2. 商业字典：先数据后类型
DELETE FROM `system_dict_data` WHERE SUBSTRING_INDEX(`dict_type`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

DELETE FROM `system_dict_type` WHERE SUBSTRING_INDEX(`type`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

-- 3. 租户套餐：按现存菜单重建 menu_ids，只处理确实含悬空 id 的套餐
DROP TEMPORARY TABLE IF EXISTS `tmp_v214_stale_package`;
DROP TEMPORARY TABLE IF EXISTS `tmp_v214_package_menu`;

CREATE TEMPORARY TABLE `tmp_v214_stale_package` (
    `id` bigint NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

-- CASE 兜底非法 JSON：非法值不参与重建，避免整条迁移因脏数据中断。
INSERT INTO `tmp_v214_stale_package` (`id`)
SELECT DISTINCT p.`id`
  FROM `system_tenant_package` p
  JOIN JSON_TABLE(
         CASE WHEN JSON_VALID(p.`menu_ids`) THEN p.`menu_ids` ELSE JSON_ARRAY() END,
         '$[*]' COLUMNS (`menu_id` bigint PATH '$')
       ) j
  LEFT JOIN `system_menu` m ON m.`id` = j.`menu_id`
 WHERE m.`id` IS NULL;

SET SESSION group_concat_max_len = 1000000;

CREATE TEMPORARY TABLE `tmp_v214_package_menu` (
    `id` bigint NOT NULL PRIMARY KEY,
    `menu_ids` varchar(4096) NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_v214_package_menu` (`id`, `menu_ids`)
SELECT p.`id`,
       CONCAT('[', COALESCE(GROUP_CONCAT(
           CASE WHEN m.`id` IS NOT NULL THEN j.`menu_id` END
           ORDER BY j.`ord` SEPARATOR ','), ''), ']')
  FROM `system_tenant_package` p
  JOIN `tmp_v214_stale_package` s ON s.`id` = p.`id`
  JOIN JSON_TABLE(
         CASE WHEN JSON_VALID(p.`menu_ids`) THEN p.`menu_ids` ELSE JSON_ARRAY() END,
         '$[*]' COLUMNS (
             `ord` FOR ORDINALITY,
             `menu_id` bigint PATH '$'
         )
       ) j
  LEFT JOIN `system_menu` m ON m.`id` = j.`menu_id`
 GROUP BY p.`id`;

UPDATE `system_tenant_package` p
  JOIN `tmp_v214_package_menu` t ON t.`id` = p.`id`
   SET p.`menu_ids` = t.`menu_ids`;

DROP TEMPORARY TABLE `tmp_v214_package_menu`;
DROP TEMPORARY TABLE `tmp_v214_stale_package`;

-- 4. 角色菜单：清掉已建租户从陈旧套餐继承的孤儿行
DELETE rm
  FROM `system_role_menu` rm
  LEFT JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE m.`id` IS NULL;

-- 自检 1：商业字典清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_NO_COMMERCIAL_DICTS PASS'
            ELSE CONCAT('V214_NO_COMMERCIAL_DICTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE SUBSTRING_INDEX(`type`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

-- 自检 2：商业站内信模板与消息清零
SELECT CASE WHEN
    (SELECT COUNT(*) FROM `system_notify_template`
      WHERE SUBSTRING_INDEX(`code`, '_', 1) IN (
            'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
            'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product')) = 0
AND (SELECT COUNT(*) FROM `system_notify_message`
      WHERE SUBSTRING_INDEX(`template_code`, '_', 1) IN (
            'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
            'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product')) = 0
THEN 'V214_NO_COMMERCIAL_NOTIFY PASS'
ELSE 'V214_NO_COMMERCIAL_NOTIFY FAIL' END AS check_result;

-- 自检 3：套餐 menu_ids 不再指向不存在的菜单
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_NO_STALE_PACKAGE_MENUS PASS'
            ELSE CONCAT('V214_NO_STALE_PACKAGE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_tenant_package` p
  JOIN JSON_TABLE(
         CASE WHEN JSON_VALID(p.`menu_ids`) THEN p.`menu_ids` ELSE JSON_ARRAY() END,
         '$[*]' COLUMNS (`menu_id` bigint PATH '$')
       ) j
  LEFT JOIN `system_menu` m ON m.`id` = j.`menu_id`
 WHERE m.`id` IS NULL;

-- 自检 4：角色菜单无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_NO_ORPHAN_ROLE_MENUS PASS'
            ELSE CONCAT('V214_NO_ORPHAN_ROLE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu` rm
  LEFT JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE m.`id` IS NULL;

-- 自检 5：套餐仍保留有效菜单（防止过滤逻辑误清空）
SELECT CASE WHEN COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0) >= 100
            THEN CONCAT('V214_PACKAGE_MENUS_INTACT PASS ', COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0))
            ELSE CONCAT('V214_PACKAGE_MENUS_INTACT FAIL ', COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0)) END AS check_result
  FROM `system_tenant_package`;
