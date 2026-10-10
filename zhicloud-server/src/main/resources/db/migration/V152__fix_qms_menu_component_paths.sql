-- ============================================================
-- V152：修正 QMS 菜单指向的前端组件路径（菜单在、页面也在，只是路径写错）
-- ============================================================
-- 背景（2026-10-10 全量核实）：应用启动后实际返回给前端的菜单里，有一批 type=2 菜单的
--   component 指向的 .vue 页面**不存在**，点进去加载不出内容。逐条比对后分两类：
--     (a) 页面确实存在、只是菜单写错了路径 —— 本次修的就是这一类（QMS 18 条）；
--     (b) 页面确实不存在 —— 属新建页面的工作量，不在本迁移（另见台账登记）。
--
-- QMS 的具体错法：V134 按「<模块>/<资源短名>/index」生成 component，但前端 QMS 页面
--   实际落在带子域的目录（如 qms/audit/plan、qms/sqm/scar、qms/training/plan）。
--   后端端点与页面 API 调用都是对的（例如 7510 页面调 /qms/audit-plan/*，后端确有该控制器），
--   所以这是**纯菜单数据错误**，改 component 即恢复，无需改页面或后端。
--
-- 幂等：按主键 id 精确 UPDATE，且只在 component 仍是旧错值时才改（重复执行 no-op）。
-- 只认 deleted=b'0' 的行。不新增/删除任何菜单行，不动按钮权限点（type=3）。
-- 列集合按 information_schema 核对：system_menu 无 tenant_id（全局表）。
-- ============================================================

UPDATE `system_menu`
   SET `component` = CASE `id`
       WHEN 7510 THEN 'qms/audit/plan/index'
       WHEN 7520 THEN 'qms/capa/document/index'
       WHEN 7530 THEN 'qms/complaint/customer/index'
       WHEN 7550 THEN 'qms/eightd/report/index'
       WHEN 7570 THEN 'qms/fmea/document/index'
       WHEN 7580 THEN 'qms/inspection/item/index'
       WHEN 7590 THEN 'qms/inspection/order/index'
       WHEN 7600 THEN 'qms/inspection/record/index'
       WHEN 7610 THEN 'qms/instrument/list/index'
       WHEN 7620 THEN 'qms/instrument/calibration/index'
       WHEN 7630 THEN 'qms/msa/study/index'
       WHEN 7640 THEN 'qms/ncr/document/index'
       WHEN 7650 THEN 'qms/training/qualification/index'
       WHEN 7670 THEN 'qms/sqm/scar/index'
       WHEN 7690 THEN 'qms/sqm/supplier-audit/index'
       WHEN 7700 THEN 'qms/sqm/supplier-rating/index'
       WHEN 7720 THEN 'qms/training/plan/index'
       WHEN 7730 THEN 'qms/training/record/index'
       ELSE `component`
     END,
     `updater` = 'system',
     `update_time` = NOW()
 WHERE `deleted` = b'0'
   AND `id` IN (7510,7520,7530,7550,7570,7580,7590,7600,7610,7620,7630,7640,7650,7670,7690,7700,7720,7730)
   AND `component` <> CASE `id`
       WHEN 7510 THEN 'qms/audit/plan/index'
       WHEN 7520 THEN 'qms/capa/document/index'
       WHEN 7530 THEN 'qms/complaint/customer/index'
       WHEN 7550 THEN 'qms/eightd/report/index'
       WHEN 7570 THEN 'qms/fmea/document/index'
       WHEN 7580 THEN 'qms/inspection/item/index'
       WHEN 7590 THEN 'qms/inspection/order/index'
       WHEN 7600 THEN 'qms/inspection/record/index'
       WHEN 7610 THEN 'qms/instrument/list/index'
       WHEN 7620 THEN 'qms/instrument/calibration/index'
       WHEN 7630 THEN 'qms/msa/study/index'
       WHEN 7640 THEN 'qms/ncr/document/index'
       WHEN 7650 THEN 'qms/training/qualification/index'
       WHEN 7670 THEN 'qms/sqm/scar/index'
       WHEN 7690 THEN 'qms/sqm/supplier-audit/index'
       WHEN 7700 THEN 'qms/sqm/supplier-rating/index'
       WHEN 7720 THEN 'qms/training/plan/index'
       WHEN 7730 THEN 'qms/training/record/index'
       ELSE `component`
     END;

-- 自检 1：这 18 条菜单的 component 都已落在新路径上（缺一即仍有死菜单）
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

-- 自检 2：本迁移只改 component，未新增/删除菜单行（QMS 目录下的菜单数不变，仍为 23）
SELECT CASE WHEN SUM(m = 'qms') = 23 THEN 'V152_QMS_MENU_COUNT PASS'
            ELSE CONCAT('V152_QMS_MENU_COUNT FAIL ', SUM(m = 'qms')) END AS check_result
  FROM (SELECT SUBSTRING_INDEX(`component`, '/', 1) AS m
          FROM `system_menu`
         WHERE `deleted` = b'0' AND `type` = 2
           AND SUBSTRING_INDEX(`component`, '/', 1) = 'qms') x;
