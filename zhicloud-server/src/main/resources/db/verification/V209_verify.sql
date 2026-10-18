-- V209 验证：FMS 期初余额重建的净效果（独立复核面）
-- 分工：V209 内自检已覆盖「表/列/唯一键/菜单」；本文件复核 Flyway 通道记录 + 独立重断言。
-- 运行口径同 V140_verify：逐条 SELECT 出的 check_result 必须全为 PASS（E2E 脚本解析）。

-- 1. Flyway 通道记录：V209 必须由应用自带 Flyway 成功执行
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '209' AND success = 1) = 1
            THEN 'V209_FLYWAY_APPLIED PASS'
            ELSE 'V209_FLYWAY_APPLIED FAIL 版本 209 不在成功记录里' END AS check_result;

-- 2. 新表与四金额列（information_schema 实测）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V209_VERIFY_TABLE_PASS'
            ELSE CONCAT('V209_VERIFY_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance';
SELECT CASE WHEN COUNT(*) = 4 THEN 'V209_VERIFY_AMOUNT_COLUMNS_PASS'
            ELSE CONCAT('V209_VERIFY_AMOUNT_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance'
   AND column_name IN ('opening_amount','year_debit_amount','year_credit_amount','profit_loss_amount');

-- 3. 唯一键（导 upsert 幂等 / 并发防重）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V209_VERIFY_UNIQUE_KEY_PASS'
            ELSE CONCAT('V209_VERIFY_UNIQUE_KEY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance'
   AND index_name = 'uk_erp_gl_initial_balance_tenant_set_account';

-- 4. 菜单与按钮权限（ERP 侧串；FMS 侧无平行串）
SELECT CASE WHEN COUNT(DISTINCT permission) = 4 THEN 'V209_VERIFY_MENU_PASS'
            ELSE CONCAT('V209_VERIFY_MENU_FAIL ', COUNT(DISTINCT permission)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `permission` IN ('erp:gl-initial-balance:query','erp:gl-initial-balance:update',
                        'erp:gl-initial-balance:import','erp:gl-initial-balance:export');
SELECT CASE WHEN COUNT(*) = 1 THEN 'V209_VERIFY_MENU_COMPONENT_PASS'
            ELSE CONCAT('V209_VERIFY_MENU_COMPONENT_FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `component` = 'fms/config/initial-balance/index';

-- 5. 两轨并存静默面：erp_gl_account 六个余额列未被本迁移改动（仍全部在）
SELECT CASE WHEN COUNT(*) = 6 THEN 'V209_VERIFY_TENANT_TRACK_INTACT PASS'
            ELSE CONCAT('V209_VERIFY_TENANT_TRACK_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_account'
   AND column_name IN ('opening_debit','opening_credit','current_debit','current_credit','closing_debit','closing_credit');
