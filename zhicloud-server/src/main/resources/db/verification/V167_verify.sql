-- V167 验证：HRM 考勤菜单 + 字典种子落库
SELECT CASE WHEN COUNT(*) = 17 THEN 'V167_ATT_MENUS PASS'
            ELSE CONCAT('V167_ATT_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `id` IN (9130,9131,9132,9133,9134,9140,9141,9150,9151,9160,9161,9162,9163,9170,9171,9172,9173);

SELECT CASE WHEN COUNT(*) = 9 THEN 'V167_ATT_DICT_TYPES PASS'
            ELSE CONCAT('V167_ATT_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0' AND `type` LIKE 'hrm_attendance%';

-- 字典数据条数：clock_type 2 + clock_source 2 + clock_status 5 + holiday_type 2 + leave_type 8
--              + yes_no 2 + late_early 3 + absenteeism 1 + misscard 1 = 26
SELECT CASE WHEN COUNT(*) = 26 THEN 'V167_ATT_DICT_DATA PASS'
            ELSE CONCAT('V167_ATT_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` LIKE 'hrm_attendance%';

-- 请假类型值域与 hr_leave_type.code 对齐（8 个 code 全覆盖）
SELECT CASE WHEN COUNT(*) = 8 THEN 'V167_LEAVE_TYPE_ALIGNED PASS'
            ELSE CONCAT('V167_LEAVE_TYPE_ALIGNED FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data` d
 WHERE d.`deleted` = b'0' AND d.`dict_type` = 'hrm_attendance_leave_type'
   AND EXISTS (SELECT 1 FROM `hr_leave_type` t
                WHERE t.`deleted` = b'0' AND t.`tenant_id` = 1 AND t.`code` = d.`value`);

-- 菜单组件路径非空（考勤 5 个菜单页）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V167_ATT_COMPONENTS PASS'
            ELSE CONCAT('V167_ATT_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND `component` LIKE 'hrm/attendance%'
   AND (`component` IS NULL OR `component` = '');
