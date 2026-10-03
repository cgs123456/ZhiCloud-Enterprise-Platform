-- ============================================================
-- V122 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   IoT 最小闭环所需的 15 张表必须齐备，且每张表都要有
--   tenant_id（服务端 zhicloud.tenant.ignore-tables 为空，
--   租户拦截器会给所有表追加 tenant_id 条件）与自增主键 id。
-- ============================================================

SELECT 'V122_IOT_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN (
       'iot_product', 'iot_product_category', 'iot_thing_model', 'iot_device',
       'iot_device_group', 'iot_device_modbus_config', 'iot_device_modbus_point',
       'iot_alert_config', 'iot_alert_record', 'iot_scene_rule', 'iot_data_rule',
       'iot_data_sink', 'iot_ota_firmware', 'iot_ota_task', 'iot_ota_task_record'
   );

SELECT 'V122_IOT_TENANT_COLUMN' AS check_name,
       CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'iot\_%'
   AND column_name = 'tenant_id';

SELECT 'V122_IOT_AUTO_INCREMENT' AS check_name,
       CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'iot\_%'
   AND column_name = 'id'
   AND EXTRA LIKE '%auto_increment%';
