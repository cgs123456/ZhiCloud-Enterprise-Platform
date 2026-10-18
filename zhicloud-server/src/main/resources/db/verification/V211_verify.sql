-- V211 验证：wms_inventory_history 库位维度接入（行级流水）（台账 §12.269，ADR 269）

-- 1. 新列存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V211_HISTORY_LOCATION_COLUMN_PASS'
            ELSE CONCAT('V211_HISTORY_LOCATION_COLUMN_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'wms_inventory_history' AND COLUMN_NAME = 'location_id';

-- 2. 新列非空（NOT NULL）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V211_HISTORY_LOCATION_NOT_NULL_PASS'
            ELSE CONCAT('V211_HISTORY_LOCATION_NOT_NULL_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'wms_inventory_history' AND COLUMN_NAME = 'location_id'
   AND IS_NULLABLE = 'NO';

-- 3. 新列默认 0（未定位哨兵）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V211_HISTORY_LOCATION_DEFAULT_ZERO_PASS'
            ELSE CONCAT('V211_HISTORY_LOCATION_DEFAULT_ZERO_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'wms_inventory_history' AND COLUMN_NAME = 'location_id'
   AND COLUMN_DEFAULT = '0';

-- 4. 存量行回零（未定位语义；历史行不回填，仅保证无 NULL）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V211_HISTORY_LOCATION_NO_NULL_PASS'
            ELSE CONCAT('V211_HISTORY_LOCATION_NO_NULL_FAIL ', COUNT(*)) END AS check_result
  FROM wms_inventory_history
 WHERE location_id IS NULL;
