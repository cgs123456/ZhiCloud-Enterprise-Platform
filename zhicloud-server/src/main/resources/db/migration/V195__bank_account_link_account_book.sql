-- ============================================================
-- V195：银行账户挂账簿——账套视角现金流量表支持流水口径（方案 B）
-- ============================================================
-- 背景（台账 §12.214/§12.215）：账套视角三大报表上线后，现金流量表是唯一
-- 降级口径的表——erp_cash_flow 只到 bank_account_id，没有账簿关联，
-- 指定账套时只能走凭证兜底（银行科目 1001/1002 变动汇总），看不到按日期的
-- 收/付款明细，也无法做投资/筹资活动归类（classifyActivity 只在流水分支生效）。
--
-- 方案选型（§12.215 调研结论）：
--   方案 A 给 erp_cash_flow 加 account_book_id——写入点仅 1 个但**存量流水
--     无账簿可回填**（流水 → 银行账户 → 账户也没账簿），历史流水按账套仍不可见；
--   方案 B 给 erp_bank_account 加 account_book_id（本迁移）——账户配好账簿后，
--     报表侧按「账簿 → 账户集合 → 流水」过滤，**存量流水随账户配置立即可见**，
--     且不碰流水表结构、不碰唯一写入点逻辑；
--   方案 C 保持凭证兜底（零改动）——现金流表永远看不到流水口径明细。
--   选 B：加列可空、存量零影响（列空 = 不参与账套过滤，与现状一致）、
--   无数据回填风险（账户配账簿是运营动作，配一条可见一条）。
--
-- 索引口径：(tenant_id, account_book_id) 复合索引——报表侧按账簿查账户是
-- 租户内查询，与 V191 账簿挂账套的 idx_erp_account_book_set 同型。
-- ============================================================

ALTER TABLE `erp_bank_account`
  ADD COLUMN `account_book_id` BIGINT NULL COMMENT '账簿编号（可空；配了账簿的账户，其流水才参与账套视角现金流量表）' AFTER `currency_id`;

CREATE INDEX `idx_erp_bank_account_book` ON `erp_bank_account` (`tenant_id`, `account_book_id`);

-- 自检 1：列已加
SELECT CASE WHEN COUNT(*) = 1 THEN 'V195_BANK_ACCOUNT_BOOK_COLUMN PASS'
            ELSE CONCAT('V195_BANK_ACCOUNT_BOOK_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_bank_account' AND column_name = 'account_book_id';

-- 自检 2：复合索引在（COUNT(DISTINCT index_name) 口径，V192 固化——每索引列一行）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V195_BANK_ACCOUNT_BOOK_INDEX PASS'
            ELSE CONCAT('V195_BANK_ACCOUNT_BOOK_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_bank_account'
   AND index_name = 'idx_erp_bank_account_book';

-- 自检 3：存量账户 account_book_id 均为 NULL（零回填，运营后续配置）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V195_NO_BACKFILL PASS'
            ELSE CONCAT('V195_NO_BACKFILL FAIL ', COUNT(*)) END AS check_result
  FROM `erp_bank_account` WHERE `account_book_id` IS NOT NULL;

-- 自检 4：流水表未被改动（方案 B 不碰 erp_cash_flow 结构）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V195_CASH_FLOW_UNTOUCHED PASS'
            ELSE CONCAT('V195_CASH_FLOW_UNTOUCHED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_cash_flow' AND column_name = 'account_book_id';
