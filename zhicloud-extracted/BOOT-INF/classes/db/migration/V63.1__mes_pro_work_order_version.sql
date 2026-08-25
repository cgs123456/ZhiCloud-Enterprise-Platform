-- P2 聚合根乐观锁：MES 生产工单表增加版本号
ALTER TABLE mes_pro_work_order
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（P2 @Version 并发保护）';
