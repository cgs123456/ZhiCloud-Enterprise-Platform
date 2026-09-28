-- ============================================================================
-- 智云企业数字化中台 —— DEMO 模拟数据不变式校验（MySQL，只读）
-- 文件: demo-data/expectations/invariants.sql
-- 配套: demo-data/generators/generate_master_data.py（默认 seed=20260928 / tenant=9001 / scale=small）
-- 状态: STATIC_ONLY —— 从未在任何数据库执行过，未验证可执行性
-- ============================================================================
--
-- 【约定】
--   1. 本文件全部为 SELECT，不含任何 DML / DDL。
--   2. 绝大多数查询的语义是「返回违规行」，因此**期望结果为 0 行**。
--      少数标了 [INFO] 的查询用于取值核对，期望返回明确数值。
--   3. 若生成时使用了 --tenant / --scale，请把文中 9001 替换为实际租户号，
--      并把 [INFO] 的期望计数替换为 manifest（demo_master_data_summary.json）中的实际值。
--   4. 生产库禁止执行：无写入风险，但会在大表上做全表聚合，避免对生产实例施压。
-- ============================================================================


-- ---------------------------------------------------------------------------
-- C1 租户隔离：creator='demo-seed' 的数据必须只存在于演示租户（期望 0 行）
-- ---------------------------------------------------------------------------
SELECT 'erp_product_category' AS tbl, COUNT(*) AS leak FROM erp_product_category WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'erp_product_unit',  COUNT(*) FROM erp_product_unit  WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'erp_product',       COUNT(*) FROM erp_product       WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'erp_customer',      COUNT(*) FROM erp_customer      WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'erp_supplier',      COUNT(*) FROM erp_supplier      WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'erp_warehouse',     COUNT(*) FROM erp_warehouse     WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_warehouse',     COUNT(*) FROM wms_warehouse     WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_zone',          COUNT(*) FROM wms_zone          WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_location',      COUNT(*) FROM wms_location      WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_item_category', COUNT(*) FROM wms_item_category WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_item',          COUNT(*) FROM wms_item          WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'wms_merchant',      COUNT(*) FROM wms_merchant      WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'crm_customer',      COUNT(*) FROM crm_customer      WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'hr_department',     COUNT(*) FROM hr_department     WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'hr_position',       COUNT(*) FROM hr_position       WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'hr_employee',       COUNT(*) FROM hr_employee       WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'tms_carrier',       COUNT(*) FROM tms_carrier       WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'tms_driver',        COUNT(*) FROM tms_driver        WHERE creator = 'demo-seed' AND tenant_id <> 9001
UNION ALL SELECT 'tms_vehicle',       COUNT(*) FROM tms_vehicle       WHERE creator = 'demo-seed' AND tenant_id <> 9001;


-- ---------------------------------------------------------------------------
-- C2 演示租户纯度：租户 9001 下不应混入非本生成器写入的数据（期望 0 行）
-- ---------------------------------------------------------------------------
SELECT 'erp_product_category' AS tbl, COUNT(*) AS foreign_rows FROM erp_product_category WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'erp_product_unit',  COUNT(*) FROM erp_product_unit  WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'erp_product',       COUNT(*) FROM erp_product       WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'erp_customer',      COUNT(*) FROM erp_customer      WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'erp_supplier',      COUNT(*) FROM erp_supplier      WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'erp_warehouse',     COUNT(*) FROM erp_warehouse     WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_warehouse',     COUNT(*) FROM wms_warehouse     WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_zone',          COUNT(*) FROM wms_zone          WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_location',      COUNT(*) FROM wms_location      WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_item_category', COUNT(*) FROM wms_item_category WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_item',          COUNT(*) FROM wms_item          WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'wms_merchant',      COUNT(*) FROM wms_merchant      WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'crm_customer',      COUNT(*) FROM crm_customer      WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'hr_department',     COUNT(*) FROM hr_department     WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'hr_position',       COUNT(*) FROM hr_position       WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'hr_employee',       COUNT(*) FROM hr_employee       WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'tms_carrier',       COUNT(*) FROM tms_carrier       WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'tms_driver',        COUNT(*) FROM tms_driver        WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed')
UNION ALL SELECT 'tms_vehicle',       COUNT(*) FROM tms_vehicle       WHERE tenant_id = 9001 AND (creator IS NULL OR creator <> 'demo-seed');


-- ---------------------------------------------------------------------------
-- C3 [INFO] 行数核对：每行应等于 manifest 的 rows 字段（small 档合计 267）
-- ---------------------------------------------------------------------------
SELECT 'erp_product_category' AS tbl, COUNT(*) AS rows_cnt, 8  AS expected FROM erp_product_category WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_product_unit',  COUNT(*), 8  FROM erp_product_unit  WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_product',       COUNT(*), 24 FROM erp_product       WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_customer',      COUNT(*), 12 FROM erp_customer      WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_supplier',      COUNT(*), 10 FROM erp_supplier      WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_warehouse',     COUNT(*), 4  FROM erp_warehouse     WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_warehouse',     COUNT(*), 4  FROM wms_warehouse     WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_zone',          COUNT(*), 12 FROM wms_zone          WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_location',      COUNT(*), 48 FROM wms_location      WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_item_category', COUNT(*), 6  FROM wms_item_category WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_item',          COUNT(*), 20 FROM wms_item          WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_merchant',      COUNT(*), 8  FROM wms_merchant      WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_customer',      COUNT(*), 15 FROM crm_customer      WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_department',     COUNT(*), 8  FROM hr_department     WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_position',       COUNT(*), 10 FROM hr_position       WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_employee',       COUNT(*), 40 FROM hr_employee       WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_carrier',       COUNT(*), 6  FROM tms_carrier       WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_driver',        COUNT(*), 12 FROM tms_driver        WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_vehicle',       COUNT(*), 12 FROM tms_vehicle       WHERE tenant_id = 9001 AND creator = 'demo-seed';


-- ---------------------------------------------------------------------------
-- C4 DEMO_ 前缀覆盖（期望 0 行）
--   名称 / 编码统一以 DEMO_ 开头，便于人工一眼识别模拟数据
-- ---------------------------------------------------------------------------
SELECT 'erp_product_category' AS tbl, COUNT(*) AS bad FROM erp_product_category WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'erp_product_unit',  COUNT(*) FROM erp_product_unit  WHERE tenant_id = 9001 AND name NOT LIKE 'DEMO_%'
UNION ALL SELECT 'erp_product',       COUNT(*) FROM erp_product       WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'erp_customer',      COUNT(*) FROM erp_customer      WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'erp_supplier',      COUNT(*) FROM erp_supplier      WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'erp_warehouse',     COUNT(*) FROM erp_warehouse     WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_warehouse',     COUNT(*) FROM wms_warehouse     WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_zone',          COUNT(*) FROM wms_zone          WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_location',      COUNT(*) FROM wms_location      WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_item_category', COUNT(*) FROM wms_item_category WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_item',          COUNT(*) FROM wms_item          WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'wms_merchant',      COUNT(*) FROM wms_merchant      WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'hr_department',     COUNT(*) FROM hr_department     WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'hr_position',       COUNT(*) FROM hr_position       WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'hr_employee',       COUNT(*) FROM hr_employee       WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR emp_no NOT LIKE 'DEMO_%')
UNION ALL SELECT 'tms_carrier',       COUNT(*) FROM tms_carrier       WHERE tenant_id = 9001 AND (name NOT LIKE 'DEMO_%' OR code NOT LIKE 'DEMO_%')
UNION ALL SELECT 'tms_vehicle',       COUNT(*) FROM tms_vehicle       WHERE tenant_id = 9001 AND plate_no NOT LIKE 'DEMO%';


-- ---------------------------------------------------------------------------
-- C5 deleted 取值（期望 0 行）
--   BIT(1)  列必须只可能是 b'0'
--   TINYINT 列必须只可能是 0
--   （生成器刻意不产出已删除行，清理脚本才能用 deleted 无关的双谓词稳定回收）
-- ---------------------------------------------------------------------------
-- BIT(1) 组
SELECT 'erp_product_category' AS tbl, COUNT(*) AS bad FROM erp_product_category WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'erp_product_unit',  COUNT(*) FROM erp_product_unit  WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'erp_product',       COUNT(*) FROM erp_product       WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'erp_customer',      COUNT(*) FROM erp_customer      WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'erp_supplier',      COUNT(*) FROM erp_supplier      WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'erp_warehouse',     COUNT(*) FROM erp_warehouse     WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'wms_warehouse',     COUNT(*) FROM wms_warehouse     WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'wms_item_category', COUNT(*) FROM wms_item_category WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'wms_item',          COUNT(*) FROM wms_item          WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'wms_merchant',      COUNT(*) FROM wms_merchant      WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'crm_customer',      COUNT(*) FROM crm_customer      WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'crm_customer.follow_up_status', COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND follow_up_status <> b'0'
UNION ALL SELECT 'hr_department',     COUNT(*) FROM hr_department     WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'hr_position',       COUNT(*) FROM hr_position       WHERE tenant_id = 9001 AND deleted <> b'0'
UNION ALL SELECT 'hr_employee',       COUNT(*) FROM hr_employee       WHERE tenant_id = 9001 AND deleted <> b'0';

-- TINYINT(1) 组
SELECT 'wms_zone'     AS tbl, COUNT(*) AS bad FROM wms_zone     WHERE tenant_id = 9001 AND deleted <> 0
UNION ALL SELECT 'wms_location', COUNT(*) FROM wms_location WHERE tenant_id = 9001 AND deleted <> 0
UNION ALL SELECT 'tms_carrier',  COUNT(*) FROM tms_carrier  WHERE tenant_id = 9001 AND deleted <> 0
UNION ALL SELECT 'tms_driver',   COUNT(*) FROM tms_driver   WHERE tenant_id = 9001 AND deleted <> 0
UNION ALL SELECT 'tms_vehicle',  COUNT(*) FROM tms_vehicle  WHERE tenant_id = 9001 AND deleted <> 0;


-- ---------------------------------------------------------------------------
-- C6 枚举域（期望 0 行）
--   TMS 状态用 10/20/30；HR 用 10/20/30/40/50；ERP/WMS 主数据 status=0
--   wms_location.status=10（空闲）；crm_customer.level=1/2/3；version=0（乐观锁）
-- ---------------------------------------------------------------------------
SELECT 'tms_carrier.status'  AS chk, COUNT(*) AS bad FROM tms_carrier  WHERE tenant_id = 9001 AND status NOT IN (10, 20, 30)
UNION ALL SELECT 'tms_driver.status',   COUNT(*) FROM tms_driver   WHERE tenant_id = 9001 AND status NOT IN (10, 20, 30)
UNION ALL SELECT 'tms_vehicle.status',  COUNT(*) FROM tms_vehicle  WHERE tenant_id = 9001 AND status NOT IN (10, 20, 30)
UNION ALL SELECT 'hr_department.status',COUNT(*) FROM hr_department WHERE tenant_id = 9001 AND status <> 10
UNION ALL SELECT 'hr_employee.status',  COUNT(*) FROM hr_employee  WHERE tenant_id = 9001 AND status NOT IN (10, 20, 30, 40)
UNION ALL SELECT 'hr_employee.gender',  COUNT(*) FROM hr_employee  WHERE tenant_id = 9001 AND gender NOT IN (10, 20)
UNION ALL SELECT 'hr_position.level',   COUNT(*) FROM hr_position  WHERE tenant_id = 9001 AND level NOT IN (10, 20, 30, 40, 50)
UNION ALL SELECT 'wms_zone.type',       COUNT(*) FROM wms_zone     WHERE tenant_id = 9001 AND type NOT IN (10, 20, 30, 40)
UNION ALL SELECT 'wms_location.type',   COUNT(*) FROM wms_location WHERE tenant_id = 9001 AND type NOT IN (10, 20, 30, 40)
UNION ALL SELECT 'wms_location.status', COUNT(*) FROM wms_location WHERE tenant_id = 9001 AND status <> 10
UNION ALL SELECT 'wms_merchant.type',   COUNT(*) FROM wms_merchant WHERE tenant_id = 9001 AND type NOT IN (1, 2, 3)
UNION ALL SELECT 'erp_product_category.status', COUNT(*) FROM erp_product_category WHERE tenant_id = 9001 AND status <> 0
UNION ALL SELECT 'erp_product.status',  COUNT(*) FROM erp_product  WHERE tenant_id = 9001 AND status <> 0
UNION ALL SELECT 'erp_customer.status', COUNT(*) FROM erp_customer WHERE tenant_id = 9001 AND status <> 0
UNION ALL SELECT 'erp_supplier.status', COUNT(*) FROM erp_supplier WHERE tenant_id = 9001 AND status <> 0
UNION ALL SELECT 'crm_customer.level',  COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND level NOT IN (1, 2, 3)
UNION ALL SELECT 'crm_customer.version',COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND version <> 0;


-- ---------------------------------------------------------------------------
-- C7 唯一键去重（期望 0 行）
--   覆盖快照中的联合唯一索引，防止同租户内编码重复导致导入即失败
-- ---------------------------------------------------------------------------
SELECT 'tms_carrier(tenant,code)'      AS uk, COUNT(*) AS dup_groups FROM (SELECT tenant_id, code FROM tms_carrier WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) x
UNION ALL SELECT 'tms_vehicle(tenant,plate_no)', COUNT(*) FROM (SELECT tenant_id, plate_no FROM tms_vehicle WHERE tenant_id = 9001 GROUP BY tenant_id, plate_no HAVING COUNT(*) > 1) y
UNION ALL SELECT 'tms_driver(tenant,license_no)',COUNT(*) FROM (SELECT tenant_id, license_no FROM tms_driver WHERE tenant_id = 9001 GROUP BY tenant_id, license_no HAVING COUNT(*) > 1) z
UNION ALL SELECT 'wms_zone(warehouse_id,code)',  COUNT(*) FROM (SELECT warehouse_id, code FROM wms_zone WHERE tenant_id = 9001 GROUP BY warehouse_id, code HAVING COUNT(*) > 1) w
UNION ALL SELECT 'erp_product(tenant,code)',     COUNT(*) FROM (SELECT tenant_id, code FROM erp_product WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) p
UNION ALL SELECT 'erp_customer(tenant,code)',    COUNT(*) FROM (SELECT tenant_id, code FROM erp_customer WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) c
UNION ALL SELECT 'erp_supplier(tenant,code)',    COUNT(*) FROM (SELECT tenant_id, code FROM erp_supplier WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) s
UNION ALL SELECT 'hr_employee(tenant,emp_no)',   COUNT(*) FROM (SELECT tenant_id, emp_no FROM hr_employee WHERE tenant_id = 9001 GROUP BY tenant_id, emp_no HAVING COUNT(*) > 1) e
UNION ALL SELECT 'hr_department(tenant,code)',   COUNT(*) FROM (SELECT tenant_id, code FROM hr_department WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) d
UNION ALL SELECT 'wms_location(tenant,code)',    COUNT(*) FROM (SELECT tenant_id, code FROM wms_location WHERE tenant_id = 9001 GROUP BY tenant_id, code HAVING COUNT(*) > 1) l;


-- ---------------------------------------------------------------------------
-- C8 审计列非空（期望 0 行）
--   creator 必须 = 'demo-seed'（清理脚本依赖），create_time / update_time 必须非空
-- ---------------------------------------------------------------------------
SELECT 'erp_product_category' AS tbl, COUNT(*) AS bad FROM erp_product_category WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'erp_product_unit',  COUNT(*) FROM erp_product_unit  WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'erp_product',       COUNT(*) FROM erp_product       WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'erp_customer',      COUNT(*) FROM erp_customer      WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'erp_supplier',      COUNT(*) FROM erp_supplier      WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'erp_warehouse',     COUNT(*) FROM erp_warehouse     WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_warehouse',     COUNT(*) FROM wms_warehouse     WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_zone',          COUNT(*) FROM wms_zone          WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_location',      COUNT(*) FROM wms_location      WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_item_category', COUNT(*) FROM wms_item_category WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_item',          COUNT(*) FROM wms_item          WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'wms_merchant',      COUNT(*) FROM wms_merchant      WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'crm_customer',      COUNT(*) FROM crm_customer      WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'hr_department',     COUNT(*) FROM hr_department     WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'hr_position',       COUNT(*) FROM hr_position       WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'hr_employee',       COUNT(*) FROM hr_employee       WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'tms_carrier',       COUNT(*) FROM tms_carrier       WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'tms_driver',        COUNT(*) FROM tms_driver        WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL)
UNION ALL SELECT 'tms_vehicle',       COUNT(*) FROM tms_vehicle       WHERE tenant_id = 9001 AND (creator <> 'demo-seed' OR create_time IS NULL OR update_time IS NULL);


-- ---------------------------------------------------------------------------
-- C9 引用完整性（期望 0 行）
--   主数据之间的父子引用必须在本租户内可解析
-- ---------------------------------------------------------------------------
SELECT 'wms_zone.warehouse_id'   AS ref, COUNT(*) AS orphan FROM wms_zone z WHERE z.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM wms_warehouse w WHERE w.id = z.warehouse_id AND w.tenant_id = 9001)
UNION ALL SELECT 'wms_location.zone_id',     COUNT(*) FROM wms_location l WHERE l.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM wms_zone z WHERE z.id = l.zone_id AND z.tenant_id = 9001)
UNION ALL SELECT 'wms_location.warehouse_id',COUNT(*) FROM wms_location l WHERE l.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM wms_warehouse w WHERE w.id = l.warehouse_id AND w.tenant_id = 9001)
UNION ALL SELECT 'wms_item.category_id',     COUNT(*) FROM wms_item i WHERE i.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM wms_item_category c WHERE c.id = i.category_id AND c.tenant_id = 9001)
UNION ALL SELECT 'erp_product.category_id',  COUNT(*) FROM erp_product p WHERE p.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM erp_product_category c WHERE c.id = p.category_id AND c.tenant_id = 9001)
UNION ALL SELECT 'erp_product.unit_id',      COUNT(*) FROM erp_product p WHERE p.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM erp_product_unit u WHERE u.id = p.unit_id AND u.tenant_id = 9001)
UNION ALL SELECT 'hr_position.dept_id',      COUNT(*) FROM hr_position po WHERE po.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM hr_department d WHERE d.id = po.dept_id AND d.tenant_id = 9001)
UNION ALL SELECT 'hr_employee.dept_id',      COUNT(*) FROM hr_employee e WHERE e.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM hr_department d WHERE d.id = e.dept_id AND d.tenant_id = 9001)
UNION ALL SELECT 'hr_employee.position_id',  COUNT(*) FROM hr_employee e WHERE e.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM hr_position po WHERE po.id = e.position_id AND po.tenant_id = 9001)
UNION ALL SELECT 'hr_department.parent_id',  COUNT(*) FROM hr_department d WHERE d.tenant_id = 9001 AND d.parent_id IS NOT NULL AND d.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM hr_department p WHERE p.id = d.parent_id AND p.tenant_id = 9001)
UNION ALL SELECT 'erp_product_category.parent_id', COUNT(*) FROM erp_product_category c WHERE c.tenant_id = 9001 AND c.parent_id IS NOT NULL AND c.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM erp_product_category p WHERE p.id = c.parent_id AND p.tenant_id = 9001)
UNION ALL SELECT 'wms_item_category.parent_id',    COUNT(*) FROM wms_item_category c WHERE c.tenant_id = 9001 AND c.parent_id IS NOT NULL AND c.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM wms_item_category p WHERE p.id = c.parent_id AND p.tenant_id = 9001)
UNION ALL SELECT 'tms_driver.carrier_id',   COUNT(*) FROM tms_driver dr WHERE dr.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM tms_carrier ca WHERE ca.id = dr.carrier_id AND ca.tenant_id = 9001)
UNION ALL SELECT 'tms_vehicle.carrier_id',  COUNT(*) FROM tms_vehicle v WHERE v.tenant_id = 9001 AND NOT EXISTS (SELECT 1 FROM tms_carrier ca WHERE ca.id = v.carrier_id AND ca.tenant_id = 9001);


-- ---------------------------------------------------------------------------
-- C10 [INFO] 未核实字段：生成器刻意留 NULL 的字段，非 NULL 计数应全部为 0
--   一旦这些列的枚举/引用语义被确认，需同步更新生成器与本查询
-- ---------------------------------------------------------------------------
SELECT 'crm_customer.source'        AS col, COUNT(*) AS not_null_cnt FROM crm_customer WHERE tenant_id = 9001 AND source IS NOT NULL
UNION ALL SELECT 'crm_customer.industry_id',    COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND industry_id IS NOT NULL
UNION ALL SELECT 'crm_customer.area_id',        COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND area_id IS NOT NULL
UNION ALL SELECT 'crm_customer.owner_user_id',  COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND owner_user_id IS NOT NULL
UNION ALL SELECT 'tms_vehicle.driver_user_id',  COUNT(*) FROM tms_vehicle  WHERE tenant_id = 9001 AND driver_user_id IS NOT NULL
UNION ALL SELECT 'hr_employee.id_card',         COUNT(*) FROM hr_employee  WHERE tenant_id = 9001 AND id_card IS NOT NULL
UNION ALL SELECT 'hr_department.leader_id',     COUNT(*) FROM hr_department WHERE tenant_id = 9001 AND leader_id IS NOT NULL;


-- ---------------------------------------------------------------------------
-- C11 [INFO] 隐私合规：不得出现真实域名邮箱 / 真实证件号
--   期望：email 全部以 @zhicloud.demo.invalid 结尾（RFC 2606 保留域）
-- ---------------------------------------------------------------------------
SELECT 'erp_customer.email' AS chk, COUNT(*) AS bad FROM erp_customer WHERE tenant_id = 9001 AND (email IS NULL OR email NOT LIKE '%@zhicloud.demo.invalid')
UNION ALL SELECT 'erp_supplier.email', COUNT(*) FROM erp_supplier WHERE tenant_id = 9001 AND (email IS NULL OR email NOT LIKE '%@zhicloud.demo.invalid')
UNION ALL SELECT 'wms_merchant.email', COUNT(*) FROM wms_merchant WHERE tenant_id = 9001 AND (email IS NULL OR email NOT LIKE '%@zhicloud.demo.invalid')
UNION ALL SELECT 'crm_customer.email', COUNT(*) FROM crm_customer WHERE tenant_id = 9001 AND (email IS NULL OR email NOT LIKE '%@zhicloud.demo.invalid')
UNION ALL SELECT 'hr_employee.email',  COUNT(*) FROM hr_employee  WHERE tenant_id = 9001 AND (email IS NULL OR email NOT LIKE '%@zhicloud.demo.invalid');

-- ============================================================================
-- 结束：C1/C2/C4/C5/C6/C7/C8/C9 期望 0 行；C3 期望等于 expected；C10/C11 见注释
-- ============================================================================