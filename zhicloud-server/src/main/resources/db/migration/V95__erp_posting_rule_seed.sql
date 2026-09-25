-- ============================================================
-- V95: ERP 默认过账规则与默认科目模板
--
-- 目标：
--   1. 为采购入库暂估、付款过账提供 tenant_id=0 的平台默认规则。
--   2. 补齐默认模板缺少的原材料和进项税额科目。
--   3. 允许租户覆盖同编码科目和同事件类型规则；服务层仅在没有有效
--      租户规则时回退到 tenant_id=0 的默认模板。
--
-- 重要限界：
--   * 默认规则是技术可运行模板，不代表任何法人的最终会计政策。
--   * 上线前必须由财务负责人确认科目、税率、暂估口径、付款科目和版本生效日。
--   * 本迁移不生成任何历史会计事件或凭证，不回填历史业务数据。
--   * 规则行金额表达式当前只允许 amountWithoutTax、taxAmount、amountWithTax、costAmount。
-- ============================================================

-- 1. 补齐默认科目模板。V7 已有 1002 银行存款、2202 应付账款。
INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1403', '原材料', 10, 10, 1, 1, 0, 40,
       'V95 默认科目模板：采购入库暂估借记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '1403' AND deleted = 0
);

INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '22210101', '应交增值税（进项税额）', 20, 10, 1, 1, 0, 41,
       'V95 默认科目模板：采购进项税额借记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '22210101' AND deleted = 0
);

INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1122', '应收账款', 10, 10, 1, 1, 0, 42,
       'V95 默认科目模板：销售应收借记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '1122' AND deleted = 0
);
INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6001', '主营业务收入', 40, 20, 1, 1, 0, 43,
       'V95 默认科目模板：销售收入贷记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '6001' AND deleted = 0
);
INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '22210102', '应交增值税（销项税额）', 20, 20, 1, 1, 0, 44,
       'V95 默认科目模板：销售销项税额贷记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '22210102' AND deleted = 0
);
INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6401', '主营业务成本', 50, 10, 1, 1, 0, 45,
       'V95 默认科目模板：销售成本借记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '6401' AND deleted = 0
);
INSERT INTO erp_gl_account
    (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark,
     creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1405', '库存商品', 10, 10, 1, 1, 0, 46,
       'V95 默认科目模板：销售成本结转贷记科目，租户可覆盖', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_gl_account WHERE tenant_id = 0 AND code = '1405' AND deleted = 0
);
-- 2. 发布版本化默认规则头。规则不可变，修改必须发布新版本。
INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_PURCHASE_RECEIPT_ACCRUAL', 'v1', 'PURCHASE_RECEIPT_ACCRUAL', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认采购入库暂估：借原材料和进项税额，贷应付账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_PURCHASE_RECEIPT_ACCRUAL' AND version = 'v1' AND deleted = 0
);

INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_PAYMENT_POSTED', 'v1', 'PAYMENT_POSTED', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认付款过账：借应付账款，贷银行存款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_PAYMENT_POSTED' AND version = 'v1' AND deleted = 0
);

INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_SALES_SHIPMENT_POSTED', 'v1', 'SALES_SHIPMENT_POSTED', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认销售出库：借应收账款和主营业务成本，贷主营业务收入、销项税额和库存商品', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_SALES_SHIPMENT_POSTED' AND version = 'v1' AND deleted = 0
);
INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_RECEIPT_POSTED', 'v1', 'RECEIPT_POSTED', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认收款过账：借银行存款，贷应收账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_RECEIPT_POSTED' AND version = 'v1' AND deleted = 0
);
-- 3. 解析规则 ID。若规则头缺失，变量保持 NULL，后续 NOT NULL 插入会立即失败。
SET @v95_purchase_rule_id = NULL;
SELECT id INTO @v95_purchase_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_PURCHASE_RECEIPT_ACCRUAL' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;

SET @v95_payment_rule_id = NULL;
SELECT id INTO @v95_payment_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_PAYMENT_POSTED' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;

SET @v95_sales_rule_id = NULL;
SELECT id INTO @v95_sales_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_SALES_SHIPMENT_POSTED' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;
SET @v95_receipt_rule_id = NULL;
SELECT id INTO @v95_receipt_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_RECEIPT_POSTED' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;
-- 4. 幂等写入规则行。金额表达式由服务层限制为事件字段白名单。
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_purchase_rule_id, 1, 10, '1403', '', 'amountWithoutTax', NULL,
       '采购入库暂估：原材料', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_purchase_rule_id AND line_no = 1 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_purchase_rule_id, 2, 10, '22210101', '', 'taxAmount', NULL,
       '采购入库暂估：进项税额', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_purchase_rule_id AND line_no = 2 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_purchase_rule_id, 3, 20, '2202', '', 'amountWithTax', NULL,
       '采购入库暂估：应付账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_purchase_rule_id AND line_no = 3 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_payment_rule_id, 1, 10, '2202', '', 'amountWithTax', NULL,
       '付款过账：冲减应付账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_payment_rule_id AND line_no = 1 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_payment_rule_id, 2, 20, '1002', '', 'amountWithTax', NULL,
       '付款过账：银行存款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_payment_rule_id AND line_no = 2 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_sales_rule_id, 1, 10, '1122', '', 'amountWithTax', NULL,
       '销售出库：应收账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_sales_rule_id AND line_no = 1 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_sales_rule_id, 2, 10, '6401', '', 'costAmount', NULL,
       '销售出库：主营业务成本', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_sales_rule_id AND line_no = 2 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_sales_rule_id, 3, 20, '6001', '', 'amountWithoutTax', NULL,
       '销售出库：主营业务收入', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_sales_rule_id AND line_no = 3 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_sales_rule_id, 4, 20, '22210102', '', 'taxAmount', NULL,
       '销售出库：销项税额', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_sales_rule_id AND line_no = 4 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_sales_rule_id, 5, 20, '1405', '', 'costAmount', NULL,
       '销售出库：库存商品结转', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_sales_rule_id AND line_no = 5 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_receipt_rule_id, 1, 10, '1002', '', 'amountWithTax', NULL,
       '收款过账：银行存款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_receipt_rule_id AND line_no = 1 AND deleted = 0
);
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v95_receipt_rule_id, 2, 20, '1122', '', 'amountWithTax', NULL,
       '收款过账：冲减应收账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v95_receipt_rule_id AND line_no = 2 AND deleted = 0
);

SET @v95_purchase_rule_id = NULL;
SET @v95_payment_rule_id = NULL;
SET @v95_sales_rule_id = NULL;
SET @v95_receipt_rule_id = NULL;
