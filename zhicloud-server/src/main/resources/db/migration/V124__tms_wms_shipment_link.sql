-- ======================== TMS 运单与 WMS 出库单关联 ========================
-- 作者：zhicloud
-- 说明：打通「WMS 出库发运 → TMS 建运单承运 → TMS 签收 → 回传 WMS 完成出库单」需要的关联列。
--
-- 为什么需要这三列：
--   1. tms_shipment 此前只有 source_order_no（自由文本单号），没有可校验的外键位，
--      所以运单和出库单之间没有任何一致性约束，签收也无从回传。
--   2. tms_shipment.sign_time 落地真实签收时刻；此前 confirmSign 只改状态不记时间。
--   3. wms_shipment_order.waybill_id 让出库单能反查承运运单（谁在什么时间签收）。
--
-- 语义：
--   * shipment_order_id / waybill_id 允许为 NULL：TMS 也支持不挂出库单的手工运单，
--     非空时才走出库单联动校验与签收回传。
--   * 不加外键：两表分属不同模块，跨模块外键会让 WMS 无法独立清理数据，
--     关联一致性由应用层 API 校验保证。

ALTER TABLE tms_shipment
    ADD COLUMN shipment_order_id BIGINT NULL DEFAULT NULL COMMENT '来源 WMS 出库单 ID（NULL 表示手工运单）';

ALTER TABLE tms_shipment
    ADD COLUMN sign_time DATETIME NULL DEFAULT NULL COMMENT '签收时间';

ALTER TABLE wms_shipment_order
    ADD COLUMN waybill_id BIGINT NULL DEFAULT NULL COMMENT '承运 TMS 运单 ID';

CREATE INDEX idx_tms_shipment_order_id ON tms_shipment (shipment_order_id);
CREATE INDEX idx_wms_shipment_order_waybill_id ON wms_shipment_order (waybill_id);
