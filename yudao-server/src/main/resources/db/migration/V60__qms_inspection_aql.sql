-- ======================== QMS 检验单 AQL 判定增强 ========================
-- 作者：智云
-- 说明：为质检判定补齐严重度与 AQL(Ac/Re) 字段，并增加业务关联(biz_type/biz_id)，
--       支撑「入库前质检卡点」与 AQL 抽样判定（致命缺陷一票否决）。

-- 检验记录：缺陷严重度（10-致命/20-严重/30-轻微）
ALTER TABLE qms_inspection_record
    ADD COLUMN severity TINYINT NOT NULL DEFAULT 30 COMMENT '缺陷严重度：10-致命(CRITICAL)/20-严重(MAJOR)/30-轻微(MINOR)';

-- 检验单：AQL 接收数/拒收数 + 业务关联
ALTER TABLE qms_inspection_order
    ADD COLUMN acceptance_quantity INT DEFAULT NULL COMMENT 'AQL 接收数 Ac（缺陷数 <= Ac 判合格）',
    ADD COLUMN reject_quantity INT DEFAULT NULL COMMENT 'AQL 拒收数 Re（缺陷数 >= Re 判不合格）',
    ADD COLUMN biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型（PURCHASE_IN/PRODUCTION_OUT/STOCK_COUNT/OTHER）',
    ADD COLUMN biz_id BIGINT DEFAULT NULL COMMENT '业务单据 ID';

CREATE INDEX idx_qms_inspection_order_biz ON qms_inspection_order (biz_type, biz_id);
