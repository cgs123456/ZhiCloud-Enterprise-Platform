-- ============================================================
-- V206：crm_sale_order 接入 BPM 审批（§12.261 C 组第 3 项）
-- ============================================================
-- 依据：台账 §12.241.5-2（销售订单 submit 未接 BPM）+ §12.261 设计冻结
-- （ADR Temp/zc_adr_261.md）。
--
-- 内容：
--   1. crm_sale_order 加列 audit_status（审批状态：0 未提交 / 10 审批中 / 20 审核通过 / 30 审核不通过 / 40 已取消）
--   2. 存量行回填 0（列新增前创建的订单视为未提交）
--
-- 幂等：列带 information_schema 防重跑护栏（直灌重跑安全）；回填仅命中 NULL 行，天然幂等。
-- 列型对齐 crm_contract.audit_status（INT DEFAULT NULL，无库级默认值；创建/提交路径由代码显式落值）。
-- 回滚预案（如需）：DROP COLUMN audit_status（旧代码忽略该列，回退代码即可）。
-- ============================================================

-- 1. crm_sale_order 加列（护栏：列已存在则跳过）
SET @v206_col := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_sale_order' AND COLUMN_NAME = 'audit_status');
SET @v206_sql := IF(@v206_col = 0,
    'ALTER TABLE `crm_sale_order` ADD COLUMN `audit_status` INT DEFAULT NULL COMMENT \'审批状态：0未提交 10审批中 20审核通过 30审核不通过 40已取消\' AFTER `status`',
    'SELECT \'V206 skip: crm_sale_order.audit_status exists\'');
PREPARE v206_stmt FROM @v206_sql; EXECUTE v206_stmt; DEALLOCATE PREPARE v206_stmt;

-- 2. 存量行回填「未提交」（仅 NULL 行，重跑安全）
UPDATE `crm_sale_order` SET `audit_status` = 0 WHERE `audit_status` IS NULL;
