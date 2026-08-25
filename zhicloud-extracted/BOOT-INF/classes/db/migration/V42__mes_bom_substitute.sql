-- ============================================================
-- V42: MES BOM 替代料管理（P0-1）
--
-- 新增 1 张表：
--   mes_bom_substitute  BOM 替代料（按 BOM 明细行挂载，支持优先级/比例/生效失效日期）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. BOM 替代料表
CREATE TABLE IF NOT EXISTS mes_bom_substitute (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    bom_id BIGINT NOT NULL COMMENT 'BOM 主表 ID（关联 mes_md_bom.id）',
    bom_detail_id BIGINT NOT NULL COMMENT 'BOM 明细 ID（被替代的物料所在行，关联 mes_md_bom_detail.id）',
    substitute_item_id BIGINT NOT NULL COMMENT '替代物料 ID（关联 mes_md_item.id）',
    substitute_ratio DECIMAL(20,6) NOT NULL DEFAULT 1.000000 COMMENT '替代比例（1 单位原物料 = ratio 单位替代料）',
    priority INT NOT NULL DEFAULT 1 COMMENT '优先级（1=首选，2=次选...，数值越小优先级越高）',
    effective_date DATE COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 / 1 禁用，CommonStatusEnum）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_bom_substitute_bom_id (bom_id),
    KEY idx_bom_substitute_detail (bom_detail_id),
    KEY idx_bom_substitute_item (substitute_item_id),
    KEY idx_bom_substitute_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES BOM 替代料';
