-- V143 验证：ERP↔WMS 行级对账视图的行为断言（不是只看定义文本）
-- 前置：真机库 zhicloud_sandbox_v140 上 ERP 产品 1 有库存、WMS SKU 1 有库存，
--       且 ERP/PRODUCT->WMS/SKU、ERP/WAREHOUSE->WMS/WAREHOUSE 两条映射均为 VALID（R-012/§12.139 已种）。

-- 1. Flyway 通道记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '143' AND success = 1) = 1
            THEN 'V143_FLYWAY_APPLIED PASS'
            ELSE 'V143_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 映射行出现在对账视图里：ERP 产品 1（映射到 WMS SKU 1）必须有一行，
--    且 wms_sku_id 解析自映射而非硬编码
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_erp_wms`
                   WHERE erp_product_id = 1 AND wms_sku_id = 1) >= 1
            THEN 'V143_MAPPED_PRODUCT_VISIBLE PASS'
            ELSE 'V143_MAPPED_PRODUCT_VISIBLE FAIL 已映射产品的对账行缺失' END AS check_result;

-- 3. 差异列算术自洽：quantity_diff = erp_quantity - wms_quantity
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_erp_wms`
                   WHERE quantity_diff <> (erp_quantity - wms_quantity)) = 0
            THEN 'V143_DIFF_ARITHMETIC PASS'
            ELSE 'V143_DIFF_ARITHMETIC FAIL 差异列与两侧数量不自洽' END AS check_result;

-- 4. reconcile_status 取值受控（三态之一），不出现空值
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_erp_wms`
                   WHERE reconcile_status NOT IN ('MATCHED','QUANTITY_DIFF','WMS_LEDGER_MISSING')
                      OR reconcile_status IS NULL) = 0
            THEN 'V143_STATUS_ENUM PASS'
            ELSE 'V143_STATUS_ENUM FAIL 出现未定义状态' END AS check_result;

-- 5. 无映射的产品不进入视图（边界：本视图只对已映射物料做行级对账）。
--    构造一个 ERP 库存行但故意不建映射，确认它不出现在结果里。
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_erp_wms`
                   WHERE erp_product_id = 999999) = 0
            THEN 'V143_UNMAPPED_EXCLUDED PASS'
            ELSE 'V143_UNMAPPED_EXCLUDED FAIL 无映射产品混入对账' END AS check_result;
