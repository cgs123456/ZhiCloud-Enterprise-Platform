-- V164 验证：HRM 菜单种子落库（目录 1 + 菜单 2 + 按钮 8 = 11 行）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V164_HRM_DIR PASS'
            ELSE CONCAT('V164_HRM_DIR FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 9100 AND `type` = 1 AND `path` = '/hrm';

SELECT CASE WHEN COUNT(*) = 2 THEN 'V164_HRM_MENUS PASS'
            ELSE CONCAT('V164_HRM_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100;

SELECT CASE WHEN COUNT(*) = 8 THEN 'V164_HRM_BUTTONS PASS'
            ELSE CONCAT('V164_HRM_BUTTONS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 9110
   AND `permission` IN ('hrm:employee:create','hrm:employee:update','hrm:employee:delete',
                        'hrm:employee:export','hrm:employee:import','hrm:employee:config:update',
                        'hrm:employee:personal-note:create','hrm:employee:personal-note:delete');

-- 员工菜单与部门菜单必须有组件路径（否则前端 404）；部门菜单权限复用 system:dept:query
SELECT CASE WHEN COUNT(*) = 0 THEN 'V164_HRM_COMPONENTS PASS'
            ELSE CONCAT('V164_HRM_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND (`component` IS NULL OR `component` = ''
        OR `component` NOT IN ('hrm/employee/index','hrm/dept/index'));
