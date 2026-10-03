-- V133 验证：日报视图必须能区分"统计结果为 0"与"维度没填"
SELECT CASE WHEN COUNT(*) >= 1 THEN 'V133_VIEW_READABLE PASS'
            ELSE 'V133_VIEW_READABLE FAIL' END AS check_result
  FROM information_schema.VIEWS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'v_qms_inspection_status_daily'
   AND VIEW_DEFINITION LIKE '%dimension_missing_count%';

-- 原有列一个都不能少、顺序不能变（消费方按列位读取）
SELECT CASE WHEN COLUMN_NAME = 'dimension_missing_count' AND ORDINAL_POSITION = 9
            THEN 'V133_COLUMN_APPENDED_AT_TAIL PASS'
            ELSE CONCAT('V133_COLUMN_APPENDED_AT_TAIL FAIL ', COLUMN_NAME, '#', ORDINAL_POSITION) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'v_qms_inspection_status_daily'
   AND ORDINAL_POSITION = 9;

-- 可用性列必须真的把缺口量出来（当前库内检验单三维全空 → 应等于检验单行数，而不是 0）
SELECT CASE WHEN (SELECT SUM(dimension_missing_count) FROM v_qms_inspection_status_daily)
                 = (SELECT COUNT(*) FROM qms_inspection_order WHERE deleted = 0)
            THEN 'V133_GAP_IS_MEASURED PASS'
            ELSE CONCAT('V133_GAP_IS_MEASURED FAIL ',
                        (SELECT SUM(dimension_missing_count) FROM v_qms_inspection_status_daily)) END AS check_result;
