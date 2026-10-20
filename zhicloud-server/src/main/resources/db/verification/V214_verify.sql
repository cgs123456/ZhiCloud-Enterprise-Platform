-- V214 验证：V140 商业域清理的存量残留（套餐菜单/站内信/字典/角色菜单）
-- 迁移内已做自检；本文件复核 Flyway 通道、四类残留、幸存数据和商业域净效果。

-- 1. Flyway 通道：V214 必须由应用自带 Flyway 成功执行
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '214' AND success = 1) = 1
            THEN 'V214_FLYWAY_APPLIED PASS'
            ELSE 'V214_FLYWAY_APPLIED FAIL 版本 214 不在成功记录里' END AS check_result;

-- 2. 商业字典类型清零（重点 brokerage_*，其余前缀防残留）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_VERIFY_NO_COMMERCIAL_DICT_TYPES PASS'
            ELSE CONCAT('V214_VERIFY_NO_COMMERCIAL_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE SUBSTRING_INDEX(`type`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

-- 3. 商业字典数据清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_VERIFY_NO_COMMERCIAL_DICT_DATA PASS'
            ELSE CONCAT('V214_VERIFY_NO_COMMERCIAL_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE SUBSTRING_INDEX(`dict_type`, '_', 1) IN (
       'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
       'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product'
 );

-- 4. 商业站内信模板与消息清零
SELECT CASE WHEN
    (SELECT COUNT(*) FROM `system_notify_template`
      WHERE SUBSTRING_INDEX(`code`, '_', 1) IN (
            'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
            'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product')) = 0
AND (SELECT COUNT(*) FROM `system_notify_message`
      WHERE SUBSTRING_INDEX(`template_code`, '_', 1) IN (
            'brokerage', 'member', 'point', 'trade', 'pay', 'promotion', 'mall',
            'coupon', 'seckill', 'bargain', 'combination', 'discount', 'product')) = 0
THEN 'V214_VERIFY_NO_COMMERCIAL_NOTIFY PASS'
ELSE 'V214_VERIFY_NO_COMMERCIAL_NOTIFY FAIL' END AS check_result;

-- 5. 套餐 menu_ids 不再含不存在的菜单 id
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_VERIFY_NO_STALE_PACKAGE_MENUS PASS'
            ELSE CONCAT('V214_VERIFY_NO_STALE_PACKAGE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_tenant_package` p
  JOIN JSON_TABLE(
         CASE WHEN JSON_VALID(p.`menu_ids`) THEN p.`menu_ids` ELSE JSON_ARRAY() END,
         '$[*]' COLUMNS (`menu_id` bigint PATH '$')
       ) j
  LEFT JOIN `system_menu` m ON m.`id` = j.`menu_id`
 WHERE m.`id` IS NULL;

-- 6. 角色菜单无孤儿（已建租户不会带着已删菜单的授权继续运行）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V214_VERIFY_NO_ORPHAN_ROLE_MENUS PASS'
            ELSE CONCAT('V214_VERIFY_NO_ORPHAN_ROLE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu` rm
  LEFT JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE m.`id` IS NULL;

-- 7. 套餐人员配置未被误清空：标准安装套餐仍保留 >= 100 个有效菜单
SELECT CASE WHEN COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0) >= 100
            THEN CONCAT('V214_VERIFY_PACKAGE_MENUS_INTACT PASS ', COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0))
            ELSE CONCAT('V214_VERIFY_PACKAGE_MENUS_INTACT FAIL ', COALESCE(SUM(JSON_LENGTH(
                CASE WHEN JSON_VALID(`menu_ids`) THEN `menu_ids` ELSE JSON_ARRAY() END
            )), 0)) END AS check_result
  FROM `system_tenant_package`;

-- 8. 非商业字典/站内信仍在（防删除面过宽）
SELECT CASE WHEN
    (SELECT COUNT(*) FROM `system_dict_type` WHERE `deleted` = b'0') > 100
AND (SELECT COUNT(*) FROM `system_notify_message` WHERE `deleted` = b'0') >= 1
THEN 'V214_VERIFY_SURVIVORS_INTACT PASS'
ELSE 'V214_VERIFY_SURVIVORS_INTACT FAIL' END AS check_result;

-- 9. V140/V146 净效果未回退：商业菜单与商业调度仍为零
SELECT CASE WHEN
    (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0'
      AND ( `permission` LIKE 'trade:%' OR `permission` LIKE 'pay:%' OR `permission` LIKE 'member:%'
         OR `permission` LIKE 'product:%' OR `permission` LIKE 'promotion:%'
         OR `name` LIKE '%商城%' OR `name` LIKE '%会员%' OR `name` LIKE '%支付%'
         OR `name` LIKE '%分销%' OR `name` LIKE '%积分%' OR `name` LIKE '%钱包%' )) = 0
AND (SELECT COUNT(*) FROM `infra_job` WHERE `deleted` = b'0'
      AND ( `handler_name` LIKE 'pay%' OR `handler_name` LIKE 'tradeOrderAuto%'
         OR `handler_name` LIKE 'brokerage%' OR `handler_name` LIKE 'combination%'
         OR `handler_name` IN ('couponExpireJob', 'productStatisticsJob') )) = 0
THEN 'V214_VERIFY_COMMERCIAL_DOMAIN_STILL_GONE PASS'
ELSE 'V214_VERIFY_COMMERCIAL_DOMAIN_STILL_GONE FAIL' END AS check_result;
