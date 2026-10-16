-- V201 验证：TMS 运单/车辆/司机乐观锁列
-- 每条都应当返回 1；返回 0 说明列没加上。
SELECT 'tms_shipment.version' AS item,
       COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tms_shipment'
  AND COLUMN_NAME = 'version';

SELECT 'tms_vehicle.version' AS item,
       COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tms_vehicle'
  AND COLUMN_NAME = 'version';

SELECT 'tms_driver.version' AS item,
       COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tms_driver'
  AND COLUMN_NAME = 'version';
