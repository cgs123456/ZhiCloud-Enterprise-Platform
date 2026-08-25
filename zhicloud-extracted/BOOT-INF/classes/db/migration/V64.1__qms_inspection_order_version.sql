-- P2 聚合根乐观锁：QMS 检验单表增加版本号
ALTER TABLE qms_inspection_order
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（P2 @Version 并发保护）';
