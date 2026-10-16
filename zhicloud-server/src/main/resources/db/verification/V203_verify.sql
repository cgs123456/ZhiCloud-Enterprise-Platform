-- V203 验证：ERP 固定资产处置/报废按钮权限播种

SELECT CASE WHEN COUNT(*) = 1 THEN 'V203_DISPOSE_PERM PASS'
            ELSE CONCAT('V203_DISPOSE_PERM FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` = 'erp:fixed-asset:dispose';

SELECT CASE WHEN COUNT(*) = 0 THEN 'V203_NO_ORPHAN PASS'
            ELSE CONCAT('V203_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` c
 WHERE c.`deleted` = b'0' AND c.`id` = 9605
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p
                    WHERE p.`id` = c.`parent_id` AND p.`deleted` = b'0');
