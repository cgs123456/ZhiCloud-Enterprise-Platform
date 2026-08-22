-- P0-3 WMS 入库质检卡点：入库单关联 QMS 检验单
ALTER TABLE wms_receipt_order
    ADD COLUMN qc_biz_id BIGINT DEFAULT NULL COMMENT '质检关联业务 ID（可选，关联 QMS 检验单 biz_id）',
    ADD COLUMN qc_biz_type VARCHAR(32) DEFAULT 'PURCHASE_IN' COMMENT '质检业务类型（InspectionBizTypeEnum）';
