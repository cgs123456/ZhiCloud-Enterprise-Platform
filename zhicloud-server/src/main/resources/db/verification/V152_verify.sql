-- V152 验证：QMS 菜单组件路径修正后的结构不变量
-- 1) 18 条被修菜单的 component 必须落在已存在的前端页面路径上（无死菜单）
SELECT CASE WHEN COUNT(*) = 18 THEN 'V152_QMS_COMPONENTS PASS'
            ELSE CONCAT('V152_QMS_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `id` IN (7510,7520,7530,7550,7570,7580,7590,7600,7610,7620,7630,7640,7650,7670,7690,7700,7720,7730)
   AND `component` IN (
       'qms/audit/plan/index','qms/capa/document/index','qms/complaint/customer/index',
       'qms/eightd/report/index','qms/fmea/document/index','qms/inspection/item/index',
       'qms/inspection/order/index','qms/inspection/record/index','qms/instrument/list/index',
       'qms/instrument/calibration/index','qms/msa/study/index','qms/ncr/document/index',
       'qms/training/qualification/index','qms/sqm/scar/index','qms/sqm/supplier-audit/index',
       'qms/sqm/supplier-rating/index','qms/training/plan/index','qms/training/record/index');

-- 2) QMS 菜单总数不变（本迁移只改 component，不增删菜单行）
SELECT CASE WHEN SUM(m = 'qms') = 23 THEN 'V152_QMS_MENU_COUNT PASS'
            ELSE CONCAT('V152_QMS_MENU_COUNT FAIL ', SUM(m = 'qms')) END AS check_result
  FROM (SELECT SUBSTRING_INDEX(`component`, '/', 1) AS m
          FROM `system_menu`
         WHERE `deleted` = b'0' AND `type` = 2
           AND SUBSTRING_INDEX(`component`, '/', 1) = 'qms') x;

-- 3) 按钮权限点未被本迁移影响（QMS type=3 仍挂在其 type=2 菜单下，无孤儿）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V152_QMS_BUTTON_PARENT PASS'
            ELSE CONCAT('V152_QMS_BUTTON_PARENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` b
  LEFT JOIN `system_menu` p ON p.id = b.parent_id AND p.type = 2 AND p.deleted = b'0'
 WHERE b.deleted = b'0' AND b.type = 3
   AND SUBSTRING_INDEX(b.permission, ':', 1) = 'qms'
   AND p.id IS NULL;
