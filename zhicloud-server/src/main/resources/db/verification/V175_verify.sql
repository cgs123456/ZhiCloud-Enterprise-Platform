-- V175 验证：HRM 绩效菜单 + 字典种子落库
SELECT CASE WHEN COUNT(*) = 13 THEN 'V175_PERF_MENUS PASS'
            ELSE CONCAT('V175_PERF_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9206 AND 9218;

SELECT CASE WHEN COUNT(*) = 7 THEN 'V175_PERF_DICT_TYPES PASS'
            ELSE CONCAT('V175_PERF_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0' AND `type` LIKE 'hrm_performance%';

-- 字典数据：计划状态5+阶段状态11+考核阶段处理2+申诉5+计分2+上限3+是否2 = 30
SELECT CASE WHEN COUNT(*) = 30 THEN 'V175_PERF_DICT_DATA PASS'
            ELSE CONCAT('V175_PERF_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` LIKE 'hrm_performance%';

-- 三个菜单页 component 非空（计划/考核模板/结果模板；绩效考核无独立菜单页）
SELECT CASE WHEN COUNT(*) = 3 THEN 'V175_PERF_COMPONENTS PASS'
            ELSE CONCAT('V175_PERF_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND `component` IN ('hrm/performance/plan/index','hrm/performance/config/assessment-template/index',
                       'hrm/performance/config/result-template/index');
