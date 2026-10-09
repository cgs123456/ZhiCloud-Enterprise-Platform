-- 将旧版 demo 明细列回填为当前服务读写所需的新列。
-- 仅作用 tenant_id=9001；不删除旧列，避免影响既有演示数据。
START TRANSACTION;

UPDATE erp_finance_receipt_item fri
JOIN erp_sale_out so ON so.id = fri.bill_id AND so.tenant_id = fri.tenant_id
SET fri.receipt_id = fri.master_id,
    fri.biz_type = 21,
    fri.biz_id = fri.bill_id,
    fri.total_price = so.total_price,
    fri.receipted_price = 0,
    fri.receipt_price = fri.payment_price
WHERE fri.tenant_id = 9001
  AND fri.receipt_id IS NULL
  AND fri.master_id IS NOT NULL
  AND fri.bill_id IS NOT NULL;

UPDATE erp_purchase_order_items poi
JOIN erp_product p ON p.id = poi.product_id AND p.tenant_id = poi.tenant_id
SET poi.order_id = poi.master_id,
    poi.product_unit_id = p.unit_id,
    poi.product_price = poi.purchase_price,
    poi.count = poi.purchase_count,
    poi.tax_percent = 0,
    poi.tax_price = 0,
    poi.in_count = 0,
    poi.return_count = 0
WHERE poi.tenant_id = 9001
  AND poi.order_id IS NULL
  AND poi.master_id IS NOT NULL;

UPDATE erp_purchase_in_items pii
JOIN erp_purchase_in pi ON pi.id = pii.master_id AND pi.tenant_id = pii.tenant_id
LEFT JOIN erp_purchase_order_items poi
       ON poi.order_id = pi.order_id
      AND poi.product_id = pii.product_id
      AND poi.tenant_id = pii.tenant_id
LEFT JOIN erp_product p ON p.id = pii.product_id AND p.tenant_id = pii.tenant_id
LEFT JOIN (
    SELECT product_id, tenant_id, MIN(warehouse_id) AS warehouse_id
      FROM erp_stock
     GROUP BY product_id, tenant_id
) s ON s.product_id = pii.product_id AND s.tenant_id = pii.tenant_id
SET pii.in_id = pii.master_id,
    pii.order_item_id = poi.id,
    pii.warehouse_id = s.warehouse_id,
    pii.product_unit_id = p.unit_id,
    pii.product_price = pii.purchase_price,
    pii.count = pii.purchase_count,
    pii.tax_percent = 0,
    pii.tax_price = 0
WHERE pii.tenant_id = 9001
  AND pii.in_id IS NULL
  AND pii.master_id IS NOT NULL;

COMMIT;