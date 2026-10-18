-- V210 验证：FMS 账表重建的净效果（独立复核面）
-- 分工：V210 内自检覆盖「菜单在位 / 页面组件 / 权限串 / 无平行串 / 无 role_menu / 无孤儿」；
-- 本文件复核 Flyway 通道记录 + 独立重断言（页面排序、按钮挂载、幂等语义）。
-- 运行口径同 V140_verify / V209_verify：逐条 SELECT 出的 check_result 必须全为 PASS。

-- 1. Flyway 通道记录：V210 必须由应用自带 Flyway 成功执行
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '210' AND success = 1) = 1
            THEN 'V210_FLYWAY_APPLIED PASS'
            ELSE 'V210_FLYWAY_APPLIED FAIL 版本 210 不在成功记录里' END AS check_result;

-- 2. 菜单在位与父挂载（4 页面 + 2 按钮，父均为 9000）
SELECT CASE WHEN COUNT(*) = 6 THEN 'V210_VERIFY_MENU_PARENT PASS'
            ELSE CONCAT('V210_VERIFY_MENU_PARENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `parent_id` = 9000
   AND `id` IN (9081, 9082, 9083, 9084, 9085, 9086);

-- 3. 页面 component 与 sort 复核（错组件 = 白屏；错排序 = 菜单顺序乱）
SELECT CASE WHEN COUNT(*) = 4
             AND SUM(`sort` IN (10, 11, 12, 13)) = 4
             AND COUNT(DISTINCT `component`) = 4
            THEN 'V210_VERIFY_PAGES PASS'
            ELSE CONCAT('V210_VERIFY_PAGES FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` IN ('fms/ledger/detail/index', 'fms/ledger/general/index',
                       'fms/ledger/subject-balance/index', 'fms/ledger/multi-column/index');

-- 4. 按钮为 type=3 且权限串在（授权面）
SELECT CASE WHEN COUNT(*) = 2 THEN 'V210_VERIFY_BUTTONS PASS'
            ELSE CONCAT('V210_VERIFY_BUTTONS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3 AND `id` IN (9085, 9086)
   AND `permission` IN ('erp:gl-ledger:export', 'erp:gl-ledger:print');

-- 5. 权限串恰好 3 个（页面共用 query，不重复造串）
SELECT CASE WHEN COUNT(DISTINCT `permission`) = 3 THEN 'V210_VERIFY_PERMISSION PASS'
            ELSE CONCAT('V210_VERIFY_PERMISSION FAIL ', COUNT(DISTINCT `permission`)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
    AND `id` IN (9081, 9082, 9083, 9084, 9085, 9086);

-- 6. FMS 侧无平行权限串（防 fms:ledger:* 回潮）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V210_VERIFY_NO_PARALLEL_PERMISSION PASS'
            ELSE CONCAT('V210_VERIFY_NO_PARALLEL_PERMISSION FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `permission` LIKE 'fms:ledger%';

-- 7. 未种 role_menu（授权是运营动作，不在种子里伪造）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V210_VERIFY_NO_ROLE_MENU PASS'
            ELSE CONCAT('V210_VERIFY_NO_ROLE_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu`
 WHERE `menu_id` IN (9081, 9082, 9083, 9084, 9085, 9086);
