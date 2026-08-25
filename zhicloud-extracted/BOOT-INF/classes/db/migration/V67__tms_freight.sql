-- ============================================================
-- V67: TMS 运费结算模块
--
-- 新增 1 张表：
--   tms_freight   运费结算单
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS tms_freight (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '结算单号',
    shipment_id BIGINT NOT NULL COMMENT '运单编号',
    carrier_id BIGINT COMMENT '承运商编号',
    billing_method TINYINT NOT NULL COMMENT '计费方式（10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费）',
    billing_quantity DECIMAL(20,4) COMMENT '计费数量',
    unit_price DECIMAL(20,4) COMMENT '单价',
    surcharge DECIMAL(20,4) DEFAULT 0 COMMENT '附加费用',
    discount_amount DECIMAL(20,4) DEFAULT 0 COMMENT '折扣金额',
    total_amount DECIMAL(20,4) NOT NULL COMMENT '运费总额',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '结算状态（10 待审核 / 20 已审核 / 30 已结算 / 40 已驳回）',
    auditor VARCHAR(64) COMMENT '审核人',
    audit_time DATETIME COMMENT '审核时间',
    settle_time DATETIME COMMENT '结算时间',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_shipment_id (shipment_id),
    KEY idx_carrier_id (carrier_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运费结算单';
