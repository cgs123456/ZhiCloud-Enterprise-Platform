-- ======================== 业务查询视图（report/datalake 的第一批可查对象） ========================
-- 作者：zhicloud
-- 说明：本轮只解决"有没有得查"，不做报表引擎。
--       zhicloud-module-report 目前只有一个 GoView 的裸 SQL 查询入口
--       （GoViewDataServiceImpl.getDataBySQL 直接把调用方 SQL 交给 JdbcTemplate），
--       给它一组可以查的视图，比让它对着上百张业务表自由拼 SQL 安全得多。
--
-- 约定：
--   1. 视图一律带 tenant_id 透出，由上层按租户过滤（视图本身不猜租户上下文）。
--   2. 视图一律过滤 deleted = 0；软删的行不进报表。
--   3. 状态码在本视图里只做"透出"，不做语义翻译：QMS/CRM/HR 各自的状态字典不在这里解释，
--      避免视图把业务口径写死后与模块自己的枚举漂移。
--   4. 只做等值 LEFT JOIN 与分组聚合，不引入跨库/物化假设。

-- 发运—承运—签收 一体视图：出库单为什么还没完成，卡在哪一段
CREATE OR REPLACE VIEW v_wms_shipment_waybill_sign AS
SELECT o.tenant_id,
       o.id             AS shipment_order_id,
       o.no             AS shipment_order_no,
       o.status         AS shipment_order_status,
       o.warehouse_id,
       o.order_time,
       o.total_quantity,
       o.total_price,
       o.waybill_id     AS linked_waybill_id,
       t.id             AS waybill_id,
       t.no             AS waybill_no,
       t.status         AS waybill_status,
       t.sign_time,
       t.freight_amount,
       CASE WHEN t.id IS NULL THEN 0 ELSE 1 END AS has_waybill,
       CASE WHEN o.status = 99 THEN 1 ELSE 0 END AS order_finished
  FROM wms_shipment_order o
  LEFT JOIN tms_shipment t ON t.shipment_order_id = o.id AND t.deleted = 0
  -- 说明：o.no / t.no 都是"没写 charset 的表"里的列，继承库默认，两者一致，不需要显式排序规则
 WHERE o.deleted = 0;

-- 检验单按日/类型/状态计数：只统计，不解释状态含义
CREATE OR REPLACE VIEW v_qms_inspection_status_daily AS
SELECT q.tenant_id,
       DATE(q.create_time)              AS stat_date,
       q.type                           AS inspection_type,
       q.status                         AS inspection_status,
       COUNT(*)                         AS order_count,
       COUNT(DISTINCT q.product_id)     AS product_count,
       COUNT(DISTINCT q.supplier_id)    AS supplier_count,
       COUNT(DISTINCT q.work_order_id)  AS work_order_count
  FROM qms_inspection_order q
 WHERE q.deleted = 0
 GROUP BY q.tenant_id, DATE(q.create_time), q.type, q.status;

-- 在途审批单据：报销 / 合同 / 请假 三类审批各自的单据号、状态与流程实例
-- 说明：hr_leave_request 目前没有 process_instance_id（尚未接入 BPM），
--       这里以 NULL 透出，便于看清"哪些审批还游离在流程引擎之外"。
CREATE OR REPLACE VIEW v_bpm_approval_inflight AS
SELECT CAST('oa_reimburse' AS CHAR) COLLATE utf8mb4_unicode_ci   AS biz_type,
       r.id                                                          AS biz_id,
       CAST(r.no AS CHAR) COLLATE utf8mb4_unicode_ci                AS biz_no,
       r.status                                                     AS biz_status,
       CAST(r.process_instance_id AS CHAR) COLLATE utf8mb4_unicode_ci AS process_instance_id,
       r.applicant_user_id AS owner_user_id,
       r.dept_id,
       r.total_amount   AS amount,
       r.update_time
  FROM oa_reimburse r
 WHERE r.deleted = 0
UNION ALL
SELECT CAST('crm_contract' AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.id,
       CAST(c.no AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.audit_status,
       CAST(c.process_instance_id AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.owner_user_id,
       NULL,
       c.total_price,
       c.update_time
  FROM crm_contract c
 WHERE c.deleted = 0
UNION ALL
SELECT CAST('hr_leave_request' AS CHAR) COLLATE utf8mb4_unicode_ci,
       h.id,
       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci,
       h.status,
       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci,
       NULL,
       NULL,
       NULL,
       h.update_time
  FROM hr_leave_request h
 WHERE h.deleted = 0;
