-- V154 验证：积木报表 jimu_* 16 张表全部存在 + 字典/分类种子落库
-- 1) 16 张 jimu_* 表齐全（information_schema 计数）
SELECT CASE WHEN COUNT(*) = 16 THEN 'V154_JIMU_TABLES PASS'
            ELSE CONCAT('V154_JIMU_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name LIKE 'jimu\_%';

-- 2) 积木报表核心表可写入语义的关键列存在（jar 2.3.4 DAO 需要 export_log 四列）
SELECT CASE WHEN COUNT(*) = 4 THEN 'V154_EXPORT_LOG_COLS PASS'
            ELSE CONCAT('V154_EXPORT_LOG_COLS FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'jimu_report_export_log'
   AND column_name IN ('export_from', 'from_id', 'err_msg', 'from_job_name');

-- 3) 字典与报表分类种子已落库
SELECT CASE WHEN COUNT(*) >= 40 THEN 'V154_DICT_SEED PASS'
            ELSE CONCAT('V154_DICT_SEED FAIL ', COUNT(*)) END AS check_result
  FROM `jimu_dict`;
SELECT CASE WHEN COUNT(*) >= 180 THEN 'V154_DICT_ITEM_SEED PASS'
            ELSE CONCAT('V154_DICT_ITEM_SEED FAIL ', COUNT(*)) END AS check_result
  FROM `jimu_dict_item`;
SELECT CASE WHEN COUNT(*) >= 3 THEN 'V154_CATEGORY_SEED PASS'
            ELSE CONCAT('V154_CATEGORY_SEED FAIL ', COUNT(*)) END AS check_result
  FROM `jimu_report_category`;
