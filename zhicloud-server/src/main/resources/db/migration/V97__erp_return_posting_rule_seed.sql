-- ============================================================
-- V97: ERP 销售退货与采购退货默认过账规则
--
-- 目标：
--   1. 为销售退货（红字收入与成本转回）和采购退货（红字暂估）提供 tenant_id=0 默认规则。
--   2. 让退货单据审核时能生成红字分录，而不是只改库存。
--
-- 重要限界：
--   * 默认规则是技术可运行模板，不代表任何法人的最终会计政策；
--     上线前必须由财务负责人确认科目、税率、红字发票口径和版本生效日。
--   * 退货事件不引用原出库/入库事件（reversalOfEventId = 0）：退货单只关联订单，
--     无法稳定定位唯一原始事件，且部分退货不应把整张原始事件标记为已冲销。
--     整单作废走反审核冲销事件，红字分录由本规则产生。
--   * 本迁移不生成任何会计事件、凭证或历史业务数据。
--   * 规则行金额表达式当前只允许 amountWithoutTax、taxAmount、amountWithTax、costAmount。
--   * 依赖 V95 已播种的默认科目：1403、22210101、2202、1122、6001、22210102、6401、1405。
-- ============================================================

-- 1. 发布版本化默认规则头。规则不可变，修改必须发布新版本。
INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_SALES_RETURN_POSTED', 'v1', 'SALES_RETURN_POSTED', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认销售退货：借主营业务收入、销项税额和库存商品，贷应收账款和主营业务成本',
       'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_SALES_RETURN_POSTED' AND version = 'v1' AND deleted = 0
);

INSERT INTO erp_posting_rule
    (rule_code, version, event_type, condition_json, effective_from, effective_to,
     enabled, published_at, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'ERP_PURCHASE_RETURN_POSTED', 'v1', 'PURCHASE_RETURN_POSTED', NULL,
       '2026-01-01 00:00:00', NULL, 1, NOW(),
       '默认采购退货：贷原材料和进项税额，借应付账款',
       'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule
    WHERE tenant_id = 0 AND rule_code = 'ERP_PURCHASE_RETURN_POSTED' AND version = 'v1' AND deleted = 0
);

-- 2. 解析规则 ID。若规则头缺失，变量保持 NULL，后续 NOT NULL 插入会立即失败。
SET @v97_sales_return_rule_id = NULL;
SELECT id INTO @v97_sales_return_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_SALES_RETURN_POSTED' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;

SET @v97_purchase_return_rule_id = NULL;
SELECT id INTO @v97_purchase_return_rule_id
FROM erp_posting_rule
WHERE tenant_id = 0 AND rule_code = 'ERP_PURCHASE_RETURN_POSTED' AND version = 'v1' AND deleted = 0
ORDER BY id
LIMIT 1;

-- 3. 幂等写入规则行。金额表达式由服务层限制为事件字段白名单。
INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_sales_return_rule_id, 1, 10, '6001', '', 'amountWithoutTax', NULL,
       '销售退货：红字冲减主营业务收入', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_sales_return_rule_id AND line_no = 1 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_sales_return_rule_id, 2, 10, '22210102', '', 'taxAmount', NULL,
       '销售退货：红字冲减销项税额', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_sales_return_rule_id AND line_no = 2 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_sales_return_rule_id, 3, 20, '1122', '', 'amountWithTax', NULL,
       '销售退货：冲减应收账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_sales_return_rule_id AND line_no = 3 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_sales_return_rule_id, 4, 10, '1405', '', 'costAmount', NULL,
       '销售退货：库存商品回冲', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_sales_return_rule_id AND line_no = 4 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_sales_return_rule_id, 5, 20, '6401', '', 'costAmount', NULL,
       '销售退货：冲减主营业务成本', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_sales_return_rule_id AND line_no = 5 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_purchase_return_rule_id, 1, 20, '1403', '', 'amountWithoutTax', NULL,
       '采购退货：红字冲减原材料', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_purchase_return_rule_id AND line_no = 1 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_purchase_return_rule_id, 2, 20, '22210101', '', 'taxAmount', NULL,
       '采购退货：红字冲减进项税额', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_purchase_return_rule_id AND line_no = 2 AND deleted = 0
);

INSERT INTO erp_posting_rule_line
    (rule_id, line_no, entry_side, account_code, account_resolver, amount_expression,
     condition_json, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @v97_purchase_return_rule_id, 3, 10, '2202', '', 'amountWithTax', NULL,
       '采购退货：冲减应付账款', 'system', NOW(), 'system', NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM erp_posting_rule_line
    WHERE tenant_id = 0 AND rule_id = @v97_purchase_return_rule_id AND line_no = 3 AND deleted = 0
);
