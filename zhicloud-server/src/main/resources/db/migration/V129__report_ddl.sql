-- ======================== 数据报表建表脚本 ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- GoView 项目表 (GoViewProjectDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS report_go_view_project (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，数据库自增',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '项目名称',
    pic_url VARCHAR(512) COMMENT '预览图片 URL',
    content TEXT COMMENT '报表内容',
    status INT COMMENT '发布状态',
    remark VARCHAR(512) COMMENT '项目备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='GoView 项目表';
