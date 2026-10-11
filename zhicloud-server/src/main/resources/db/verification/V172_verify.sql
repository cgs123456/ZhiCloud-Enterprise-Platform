-- V172 验证：HRM 招聘菜单 + 字典种子落库
SELECT CASE WHEN COUNT(*) = 14 THEN 'V172_RECRUIT_MENUS PASS'
            ELSE CONCAT('V172_RECRUIT_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9190 AND 9203;

SELECT CASE WHEN COUNT(*) = 10 THEN 'V172_RECRUIT_DICT_TYPES PASS'
            ELSE CONCAT('V172_RECRUIT_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0' AND `type` LIKE 'hrm_recruit%';

-- 字典数据 43 条（职位状态2+工作性质3+经验6+学历6+薪资单位2+紧急2+候选人状态8+候选人学历7+面试方式3+面试结果4）
SELECT CASE WHEN COUNT(*) = 43 THEN 'V172_RECRUIT_DICT_DATA PASS'
            ELSE CONCAT('V172_RECRUIT_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` LIKE 'hrm_recruit%';

-- 三个菜单页 component 非空（职位/候选人/渠道）
SELECT CASE WHEN COUNT(*) = 3 THEN 'V172_RECRUIT_COMPONENTS PASS'
            ELSE CONCAT('V172_RECRUIT_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND `component` IN ('hrm/recruit/post/index','hrm/recruit/candidate/index','hrm/recruit/channel/index');
