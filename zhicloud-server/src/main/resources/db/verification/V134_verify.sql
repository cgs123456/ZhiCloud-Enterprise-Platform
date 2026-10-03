-- V134 验证：HR / QMS 菜单种子的结构不变量
-- 1) 两个目录存在
SELECT CASE WHEN COUNT(*) = 2 THEN 'V134_DIRS PASS'
            ELSE CONCAT('V134_DIRS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu
 WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path IN ('/hr', '/qms');

-- 2) 菜单数与代码里声明的资源数一致（13 个 HR 资源、23 个 QMS 资源）
SELECT CASE WHEN SUM(m = 'hr') = 13 AND SUM(m = 'qms') = 23 THEN 'V134_MENU_COUNTS PASS'
            ELSE CONCAT('V134_MENU_COUNTS FAIL hr=', SUM(m = 'hr'), ' qms=', SUM(m = 'qms')) END AS check_result
  FROM (SELECT SUBSTRING_INDEX(component, '/', 1) AS m
          FROM system_menu
         WHERE deleted = b'0' AND type = 2 AND SUBSTRING_INDEX(component, '/', 1) IN ('hr', 'qms')) x;

-- 3) 前端路由约定必须是 <模块>/<资源>/index，且 path 就是资源短名（与既有 wms/crm 菜单同一写法）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V134_COMPONENT_CONVENTION PASS'
            ELSE CONCAT('V134_COMPONENT_CONVENTION FAIL ', COUNT(*)) END AS check_result
  FROM system_menu
 WHERE deleted = b'0' AND type = 2 AND SUBSTRING_INDEX(component, '/', 1) IN ('hr', 'qms')
   AND (component NOT LIKE CONCAT(SUBSTRING_INDEX(component, '/', 1), '/%/index')
        OR component NOT LIKE CONCAT('%/', path, '/index'));

-- 4) (permission, type) 不得重复：重复会让"权限点是否已建菜单"这件事不可判定
SELECT CASE WHEN COUNT(*) = 0 THEN 'V134_NO_DUPLICATE_PERMISSION PASS'
            ELSE CONCAT('V134_NO_DUPLICATE_PERMISSION FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT permission, type FROM system_menu
         WHERE deleted = b'0' AND permission <> ''
           AND (SUBSTRING_INDEX(permission, ':', 1) IN ('hr', 'qms'))
         GROUP BY permission, type HAVING COUNT(*) > 1) d;

-- 5) 按钮必须挂在同子树的菜单下（孤儿按钮等于永不生效的权限点）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V134_BUTTON_PARENT PASS'
            ELSE CONCAT('V134_BUTTON_PARENT FAIL ', COUNT(*)) END AS check_result
  FROM system_menu b
  LEFT JOIN system_menu p ON p.id = b.parent_id AND p.type = 2 AND p.deleted = b'0'
 WHERE b.deleted = b'0' AND b.type = 3
   AND SUBSTRING_INDEX(b.permission, ':', 1) IN ('hr', 'qms')
   AND p.id IS NULL;
