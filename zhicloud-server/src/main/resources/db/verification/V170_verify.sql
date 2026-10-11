-- V170 验证：HRM 保险菜单与按钮种子落库（菜单 1 + 按钮 6 = 7 行）
SELECT CASE WHEN COUNT(*) = 7 THEN 'V170_INS_MENU_PASS'
            ELSE CONCAT('V170_INS_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `id` IN (9180,9181,9182,9183,9184,9185,9186);

-- 菜单页 component 非空（参保方案列表页必须有组件路径，否则前端 404）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V170_INS_COMPONENT_PASS'
            ELSE CONCAT('V170_INS_COMPONENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 9180 AND `type` = 2
   AND `component` = 'hrm/insurance/scheme/index';

-- 标准只读权限作按钮（type=3），不占菜单页
SELECT CASE WHEN COUNT(*) = 2 THEN 'V170_INS_STD_BUTTONS_PASS'
            ELSE CONCAT('V170_INS_STD_BUTTONS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` IN ('hrm:insurance:standard:type-list','hrm:insurance:standard:project-list');
