-- P2 聚合根乐观锁：WMS 库存余额表增加版本号
ALTER TABLE wms_inventory
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（P2 @Version 并发保护）';
