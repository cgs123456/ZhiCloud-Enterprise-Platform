-- V166 验证：HRM 考勤域 6 张表 + 关键唯一键落库
SELECT CASE WHEN COUNT(*) = 6 THEN 'V166_ATT_TABLES PASS'
            ELSE CONCAT('V166_ATT_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_attendance_group','hr_attendance_group_employee','hr_attendance_clock',
                        'hr_attendance_holiday','hr_attendance_leave','hr_attendance_month_record');

-- 月度汇总 (employee_id, year, month) 唯一键存在
SELECT CASE WHEN COUNT(DISTINCT `index_name`) = 1 AND COUNT(*) = 3
             THEN 'V166_MONTH_UK PASS'
            ELSE CONCAT('V166_MONTH_UK FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_attendance_month_record'
   AND `index_name` = 'uk_hr_att_month_emp';

-- 打卡 (employee_id, attendance_time) 查询索引存在
SELECT CASE WHEN COUNT(DISTINCT `index_name`) = 1 AND COUNT(*) = 2
             THEN 'V166_CLOCK_IDX PASS'
            ELSE CONCAT('V166_CLOCK_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_attendance_clock'
   AND `index_name` = 'idx_hr_att_clock_emp_time';

-- 考勤组 json 列（班次/地点/规则整体存）落列
SELECT CASE WHEN COUNT(*) = 6 THEN 'V166_GROUP_JSON_COLS PASS'
            ELSE CONCAT('V166_GROUP_JSON_COLS FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_attendance_group'
   AND `column_name` IN ('special_dates','dept_ids','employee_ids','shifts','points','wifis');
