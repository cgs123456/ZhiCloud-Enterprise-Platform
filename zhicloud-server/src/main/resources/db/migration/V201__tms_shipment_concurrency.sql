-- TMS 运单状态机并发保护（§12.231，第 10 个真实缺陷）
--
-- 背景：TmsShipmentServiceImpl 的 dispatch/confirmArrival/confirmSign 都是
-- selectById 读单据 → 内存判 status → updateById 全量覆盖；tms_shipment /
-- tms_vehicle / tms_driver 三张表都没有 version 列，zhicloud-module-tms 下
-- @Version 零命中。
--
-- 后果（并发重复派车）：两个客户端同时对同一张「待发车」运单 dispatch，
-- 两者都读到 status=10，都通过校验，都 updateById 成功——运单被写入
-- 两辆不同的车/两个不同的司机，而两次 dispatch 还会把上一辆车/司机的
-- 状态互相覆盖。用户看到两次都成功。
--
-- 修法对齐仓内既有范式 V63.1（MesProWorkOrderDO）+ commit e0169a497：
-- 先加 version 列，DO 加 @Version，Service 检查更新返回值。
-- 运单的状态推进用的是「CAS 状态条件」的 UPDATE（transitStatusIfChanged），
-- 不是 @Version 自动拼 version 的 updateById——因为 dispatch 需要
-- 「校验仍是待发车」与「写入新状态」在同一条语句里原子完成，否则失败方
-- 已经把车辆/司机的状态改成「运输中」了。
-- 车辆/司机两表用 @Version + updateByIdWithVersion（同 §12.228 的返回值检查）。

ALTER TABLE `tms_shipment`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（状态机并发保护）' AFTER `tenant_id`;

ALTER TABLE `tms_vehicle`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（状态并发保护）' AFTER `tenant_id`;

ALTER TABLE `tms_driver`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（状态并发保护）' AFTER `tenant_id`;

-- 自检：三列必须真实存在且默认值正确。列本身的校验由下面这条承担，
-- 索引/一键的正确性由 V201_verify.sql 的 information_schema 查询负责。
SELECT CASE WHEN COUNT(*) = 3 THEN 'PASS V201 columns added'
            ELSE CONCAT('FAIL V201 expected 3 new columns, got ', COUNT(*)) END AS result
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('tms_shipment', 'tms_vehicle', 'tms_driver')
  AND COLUMN_NAME = 'version'
  AND COLUMN_TYPE = 'bigint'
  AND COLUMN_DEFAULT = '0'
  AND IS_NULLABLE = 'NO';
