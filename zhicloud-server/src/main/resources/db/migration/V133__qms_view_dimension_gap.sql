-- ======================== QMS 检验日报视图补"维度可用性"列 ========================
-- 作者：zhicloud
-- 背景：V126 的 v_qms_inspection_status_daily 用 COUNT(DISTINCT product_id/supplier_id/work_order_id)
--       做维度统计。真机复核发现 qms_inspection_order 有效行的这三列**全部为 NULL**，
--       于是视图"每行都有、每个维度计数都是 0"——0 看起来像一个结论（今天检了 0 种产品），
--       实际含义是"这一列没数据、统计无从算起"。这类"代理绿"比报错更危险。
-- 做法：不改已有列的顺序与语义（消费方不位移），只在末尾追加一列可用性计数，
--       让报表页能直接区分"真的没有"与"没填"。
-- 外键契约（本轮按项目内既有声明确定，写在这里供后续补数据时对齐）：
--   supplier_id → erp_supplier.id（全仓 9 处 supplierId 的 javadoc 都关联 ErpSupplierDO）
--   product_id  → mes_md_item.id （MesProWorkOrderDO.productId 的 javadoc 明确"关联 MesMdItemDO#getId()"，
--                  QMS 追溯服务也把 productId 当 materialId 用）
--   注意 wms_receipt_order.merchant_id 是 WmsMerchantDO（商户），与"供应商"不是同一实体，不可互相回填。

CREATE OR REPLACE VIEW v_qms_inspection_status_daily AS
SELECT q.tenant_id,
       DATE(q.create_time)              AS stat_date,
       q.type                           AS inspection_type,
       q.status                         AS inspection_status,
       COUNT(*)                         AS order_count,
       COUNT(DISTINCT q.product_id)     AS product_count,
       COUNT(DISTINCT q.supplier_id)    AS supplier_count,
       COUNT(DISTINCT q.work_order_id)  AS work_order_count,
       SUM(q.product_id IS NULL OR q.supplier_id IS NULL) AS dimension_missing_count
  FROM qms_inspection_order q
 WHERE q.deleted = 0
 GROUP BY q.tenant_id, DATE(q.create_time), q.type, q.status;
