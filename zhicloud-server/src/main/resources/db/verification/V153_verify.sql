-- V153 验证：代码生成案例目录菜单的组件字段修正
-- 1) 目录（type=1）component 已清空
SELECT CASE WHEN COUNT(*) = 1 THEN 'V153_TESTDEMO_DIR_COMPONENT PASS'
            ELSE CONCAT('V153_TESTDEMO_DIR_COMPONENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 1070 AND `type` = 1 AND `component` = '';

-- 2) 5 个子菜单仍在且指向 demo 页面
SELECT CASE WHEN COUNT(*) = 5 THEN 'V153_TESTDEMO_CHILDREN PASS'
            ELSE CONCAT('V153_TESTDEMO_CHILDREN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `parent_id` = 1070
   AND `component` LIKE 'infra/demo/%';
