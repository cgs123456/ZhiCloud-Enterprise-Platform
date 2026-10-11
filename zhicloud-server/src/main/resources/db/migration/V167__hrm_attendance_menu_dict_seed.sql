-- V167: HRM 考勤域菜单与按钮权限 + 考勤字典种子
-- ============================================================
-- 一、菜单（挂 9100 /hrm 目录下，id 9130-9199 段；避开 9100-9120 员工/部门）
--     来源：views/hrm/attendance 页面 v-hasPermi 与后端 @PreAuthorize 双侧交集（非手抄清单）。
--     权限口径：clock/{create,update,delete,export}、group/{create,update,delete}、
--               holiday/{create,update,delete}、leave/export、statistics/{query,export}。
-- 二、字典 9 个（前端 DICT_TYPE 常量逐项对齐）：
--     hrm_attendance_clock_type/clock_source/clock_status/holiday_type/leave_type/yes_no/
--     late_early_deduct_method/absenteeism_deduct_method/misscard_deduct_method
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）。system_menu 无 tenant_id（全局表）；
-- system_dict_type 无 tenant_id；system_dict_data 无 tenant_id（字典为平台级共享）。
-- 幂等：菜单按 (permission,type)/path 预检；字典按 type 预检；字典数据按 (dict_type,value) 预检。
-- MySQL 8.4。

-- ========== 一、菜单 ==========

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9130 AS `id`, '考勤记录', 'hrm:attendance:clock:query' AS `permission`, 2 AS `type`, 3 AS `sort`, 9100 AS `parent_id`, 'attendance/clock' AS `path`, 'ep:alarm-clock' AS `icon`, 'hrm/attendance/clock/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9131, '打卡新增', 'hrm:attendance:clock:create', 3, 1, 9130, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9132, '打卡修改', 'hrm:attendance:clock:update', 3, 2, 9130, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9133, '打卡删除', 'hrm:attendance:clock:delete', 3, 3, 9130, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9134, '打卡导出', 'hrm:attendance:clock:export', 3, 4, 9130, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9140, '请假记录', 'hrm:attendance:leave:query', 2, 4, 9100, 'attendance/leave', 'ep:document-checked', 'hrm/attendance/leave/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9141, '请假导出', 'hrm:attendance:leave:export', 3, 1, 9140, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9150, '月度考勤', 'hrm:attendance:statistics:query', 2, 5, 9100, 'attendance/month', 'ep:calendar', 'hrm/attendance/month/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9151, '考勤汇总导出', 'hrm:attendance:statistics:export', 3, 1, 9150, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9160, '考勤组配置', 'hrm:attendance:group:query', 2, 6, 9100, 'attendance/config/group', 'ep:setting', 'hrm/attendance/config/group/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9161, '考勤组新增', 'hrm:attendance:group:create', 3, 1, 9160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9162, '考勤组修改', 'hrm:attendance:group:update', 3, 2, 9160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9163, '考勤组删除', 'hrm:attendance:group:delete', 3, 3, 9160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9170, '节假日配置', 'hrm:attendance:holiday:query', 2, 7, 9100, 'attendance/config/holiday', 'ep:date', 'hrm/attendance/config/holiday/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9171, '节假日新增', 'hrm:attendance:holiday:create', 3, 1, 9170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9172, '节假日修改', 'hrm:attendance:holiday:update', 3, 2, 9170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9173, '节假日删除', 'hrm:attendance:holiday:delete', 3, 3, 9170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ========== 二、字典类型（9 个） ==========

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 打卡类型' AS `name`, 'hrm_attendance_clock_type' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 打卡来源', 'hrm_attendance_clock_source', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 打卡状态', 'hrm_attendance_clock_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 考勤节假日类型', 'hrm_attendance_holiday_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 请假类型', 'hrm_attendance_leave_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 考勤是否', 'hrm_attendance_yes_no', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 迟到早退扣款方式', 'hrm_attendance_late_early_deduct_method', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 旷工扣款方式', 'hrm_attendance_absenteeism_deduct_method', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 缺卡扣款方式', 'hrm_attendance_misscard_deduct_method', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

-- ========== 三、字典数据（值域与前端 constants.ts / 后端枚举逐项对齐） ==========

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  -- 打卡类型（HrmAttendanceClockType：1上班 2下班）
  SELECT 1 AS `sort`, '上班打卡' AS `label`, '1' AS `value`, 'hrm_attendance_clock_type' AS `dict_type`, 0 AS `status`, 'primary' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '下班打卡', '2', 'hrm_attendance_clock_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 打卡来源（HrmAttendanceClockSource：1手机端 2手工录入）
  UNION ALL SELECT 1, '手机端', '1', 'hrm_attendance_clock_source', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '手工录入', '2', 'hrm_attendance_clock_source', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 打卡状态（后端 HrmAttendanceClockStatusEnum 口径：1正常 2迟到 3早退 4缺卡 5旷工）
  UNION ALL SELECT 1, '正常', '1', 'hrm_attendance_clock_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '迟到', '2', 'hrm_attendance_clock_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '早退', '3', 'hrm_attendance_clock_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '缺卡', '4', 'hrm_attendance_clock_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '旷工', '5', 'hrm_attendance_clock_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 节假日类型（HrmAttendanceHolidayType：1上班 2休息）
  UNION ALL SELECT 1, '上班', '1', 'hrm_attendance_holiday_type', 0, 'warning', '', '调休上班', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '休息', '2', 'hrm_attendance_holiday_type', 0, 'success', '', '法定节假日', 'system', NOW(), 'system', NOW(), b'0'
  -- 请假类型（字符串值，与 hr_leave_type.code 对齐：ANNUAL/SICK/...）
  UNION ALL SELECT 1, '年假', 'ANNUAL', 'hrm_attendance_leave_type', 0, 'success', '', '法定带薪年休假', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '病假', 'SICK', 'hrm_attendance_leave_type', 0, 'warning', '', '凭医院证明的带薪病假', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '事假', 'PERSONAL', 'hrm_attendance_leave_type', 0, 'info', '', '无薪事假，按天扣薪', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '调休', 'COMPENSATORY', 'hrm_attendance_leave_type', 0, 'primary', '', '加班转调休', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '婚假', 'MARRIAGE', 'hrm_attendance_leave_type', 0, 'danger', '', '法定婚假', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '产假', 'MATERNITY', 'hrm_attendance_leave_type', 0, 'danger', '', '法定产假', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7, '陪产假', 'PATERNITY', 'hrm_attendance_leave_type', 0, 'danger', '', '法定陪产假', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 8, '丧假', 'BEREAVEMENT', 'hrm_attendance_leave_type', 0, 'info', '', '直系亲属丧假', 'system', NOW(), 'system', NOW(), b'0'
  -- 是否（通用布尔）
  UNION ALL SELECT 1, '是', 'true', 'hrm_attendance_yes_no', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '否', 'false', 'hrm_attendance_yes_no', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 迟到早退扣款方式（HrmAttendanceLateEarlyDeductMethod：1每月固定 2按分钟 3按次数）
  UNION ALL SELECT 1, '每月固定扣款', '1', 'hrm_attendance_late_early_deduct_method', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '按分钟扣款', '2', 'hrm_attendance_late_early_deduct_method', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '按次数扣款', '3', 'hrm_attendance_late_early_deduct_method', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 旷工扣款方式（1按旷工天数）
  UNION ALL SELECT 1, '按旷工天数扣款', '1', 'hrm_attendance_absenteeism_deduct_method', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 缺卡扣款方式（1按缺卡次数）
  UNION ALL SELECT 1, '按缺卡次数扣款', '1', 'hrm_attendance_misscard_deduct_method', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);
