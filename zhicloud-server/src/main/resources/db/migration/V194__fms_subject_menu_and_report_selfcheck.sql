-- ============================================================
-- V194：FMS 语境「会计科目」菜单播种 + 账套报表/科目链路一致性自检
-- ============================================================
-- 背景（台账 §12.214）：FMS/ERP 财务融合后续增强两项落地——
--   1. 账套视角三大报表（FMS /fms/config/account-set/{balance-sheet,
--      income-statement,cash-flow-statement} 只读端点，数据经 ERP api 边界包，
--      按「账套 → 账簿 → 凭证」链路取已审核凭证）
--   2. FMS 语境会计科目维护（前端页 fms/config/subject 直连 ERP
--      /erp/gl-account CRUD 端点——erp_gl_account 是科目单一真相，
--      刻意不建 FMS 侧第二套科目表/代理 Controller）
--
-- 本迁移内容：
--   1. 种「会计科目」菜单（挂 FMS 目录 9000 下）
--   2. 自检：账簿挂账套列/索引仍在（V191）、凭证表无 account_set_id 冗余列（V193）、
--      科目表 erp_gl_account 仍在（单一真相未被破坏）、ERP 侧科目按钮权限在位
--      （erp:gl-account:{create,update,delete,query} 权限串全局唯一，FMS 页面
--      经 v-hasPermi 复用，刻意不在 FMS 树下重复挂同 permission 的按钮——
--      V158 幂等口径同 permission+type 全局一份，重复挂会被预检拦掉）
--
-- 写法沿用 V158 口径：幂等插入（菜单按 permission+type 预检），system_menu 无 tenant_id。
-- ============================================================

-- 1. 会计科目菜单（权限串与后端 @PreAuthorize 一一对应；按钮权限复用 ERP 侧已有的
--    erp:gl-account:*（id 6749-6752）——权限的单一真相，不造 fms:* 平行串）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9020 AS `id`, '会计科目' AS `name`, 'erp:gl-account:query' AS `permission`, 2 AS `type`, 0 AS `sort`, 9000 AS `parent_id`, 'config/subject' AS `path`, 'ep:collection-tag' AS `icon`, 'fms/config/subject/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- 2. 自检 1：账簿挂账套列仍在（V191 层级未被动摇）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V194_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V194_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

-- 3. 自检 2：账套→账簿复合索引仍在（COUNT(DISTINCT index_name) 口径，V192 固化）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V194_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V194_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

-- 4. 自检 3：凭证表不得有 account_set_id 冗余列（单一来源是账簿，防回归）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V194_VOUCHER_NO_REDUNDANT_COLUMN PASS'
            ELSE CONCAT('V194_VOUCHER_NO_REDUNDANT_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher' AND column_name = 'account_set_id';

-- 5. 自检 4：科目表 erp_gl_account 仍在（科目单一真相，未建第二套表）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V194_GL_ACCOUNT_INTACT PASS'
            ELSE CONCAT('V194_GL_ACCOUNT_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_gl_account';

-- 6. 自检 5：ERP 侧科目按钮权限在位（FMS 页面经 v-hasPermi 复用，不重复播种）
SELECT CASE WHEN COUNT(DISTINCT permission) = 4 THEN 'V194_GL_ACCOUNT_BUTTONS PASS'
            ELSE CONCAT('V194_GL_ACCOUNT_BUTTONS FAIL ', COUNT(DISTINCT permission)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` IN ('erp:gl-account:query', 'erp:gl-account:create',
                        'erp:gl-account:update', 'erp:gl-account:delete');

-- 7. 自检 6：FMS 侧不得出现平行科目表（fms_subject / fms_gl_account 等均不应存在）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V194_NO_PARALLEL_SUBJECT_TABLE PASS'
            ELSE CONCAT('V194_NO_PARALLEL_SUBJECT_TABLE FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN ('fms_subject', 'fms_gl_account', 'fms_account');
