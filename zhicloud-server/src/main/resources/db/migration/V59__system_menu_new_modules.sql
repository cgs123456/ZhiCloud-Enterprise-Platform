-- ============================================================
-- V59: 新模块菜单注册 system_menu
--
-- 覆盖模块：OA / TMS / ERP 询比价 / WMS 越库计费 / MES 计件SCADA / CRM 开票拜访
-- 幂等性：INSERT IGNORE + 显式 ID（3000-3099 区间在 system_menu 中未被占用）
-- 父目录引用：采购管理(2602) / CRM 系统(2397) / WMS 系统(6400) / 生产管理(5700) / 设备管理(5300)
-- 字段对齐：system_menu 实际表结构（无 tenant_id 列；deleted 用 0/1）
-- ============================================================

-- ============================================================
-- 一、OA 模块（新建顶级目录 OA管理）
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3000, 'OA管理', '', 1, 90, 0, '/oa', 'ep:office-building', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 1.1 报销管理
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3001, '报销管理', 'oa:reimburse:query', 2, 1, 3000, 'reimburse', 'ep:money', 'oa/reimburse/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3002, '报销创建', 'oa:reimburse:create', 3, 1, 3001, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3003, '报销更新', 'oa:reimburse:update', 3, 2, 3001, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3004, '报销删除', 'oa:reimburse:delete', 3, 3, 3001, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3005, '报销导出', 'oa:reimburse:export', 3, 4, 3001, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 1.2 会议室管理（含会议室预约按钮）
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3006, '会议室管理', 'oa:meeting-room:query', 2, 2, 3000, 'meeting-room', 'ep:house', 'oa/meeting-room/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3007, '会议室创建', 'oa:meeting-room:create', 3, 1, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3008, '会议室更新', 'oa:meeting-room:update', 3, 2, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3009, '会议室删除', 'oa:meeting-room:delete', 3, 3, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3010, '会议室预约创建', 'oa:meeting-reservation:create', 3, 4, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3011, '会议室预约更新', 'oa:meeting-reservation:update', 3, 5, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3012, '会议室预约删除', 'oa:meeting-reservation:delete', 3, 6, 3006, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 1.3 公文管理
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3013, '公文管理', 'oa:document:query', 2, 3, 3000, 'document', 'ep:document', 'oa/document/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3014, '公文创建', 'oa:document:create', 3, 1, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3015, '公文更新', 'oa:document:update', 3, 2, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3016, '公文删除', 'oa:document:delete', 3, 3, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3017, '公文提交', 'oa:document:submit', 3, 4, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3018, '公文发布', 'oa:document:publish', 3, 5, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3019, '公文作废', 'oa:document:void', 3, 6, 3013, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- 二、TMS 模块（新建顶级目录 运输管理）
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3020, '运输管理', '', 1, 95, 0, '/tms', 'ep:truck', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 2.1 承运商
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3021, '承运商', 'tms:carrier:query', 2, 1, 3020, 'carrier', 'ep:bicycle', 'tms/carrier/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3022, '承运商创建', 'tms:carrier:create', 3, 1, 3021, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3023, '承运商更新', 'tms:carrier:update', 3, 2, 3021, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3024, '承运商删除', 'tms:carrier:delete', 3, 3, 3021, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 2.2 车辆
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3025, '车辆', 'tms:vehicle:query', 2, 2, 3020, 'vehicle', 'ep:van', 'tms/vehicle/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3026, '车辆创建', 'tms:vehicle:create', 3, 1, 3025, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3027, '车辆更新', 'tms:vehicle:update', 3, 2, 3025, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3028, '车辆删除', 'tms:vehicle:delete', 3, 3, 3025, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 2.3 司机
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3029, '司机', 'tms:driver:query', 2, 3, 3020, 'driver', 'ep:avatar', 'tms/driver/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3030, '司机创建', 'tms:driver:create', 3, 1, 3029, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3031, '司机更新', 'tms:driver:update', 3, 2, 3029, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3032, '司机删除', 'tms:driver:delete', 3, 3, 3029, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 2.4 运单
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3033, '运单', 'tms:shipment:query', 2, 4, 3020, 'shipment', 'ep:document-copy', 'tms/shipment/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3034, '运单创建', 'tms:shipment:create', 3, 1, 3033, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3035, '运单更新', 'tms:shipment:update', 3, 2, 3033, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3036, '运单删除', 'tms:shipment:delete', 3, 3, 3033, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 2.5 跟踪
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3037, '跟踪', 'tms:tracking:query', 2, 5, 3020, 'tracking', 'ep:place', 'tms/tracking/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3038, '跟踪创建', 'tms:tracking:create', 3, 1, 3037, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3039, '跟踪更新', 'tms:tracking:update', 3, 2, 3037, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3040, '跟踪删除', 'tms:tracking:delete', 3, 3, 3037, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- 三、ERP 询比价模块（挂在 采购管理 2602 下）
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3041, '询价单', 'erp:purchase-inquiry:query', 2, 20, 2602, 'purchase-inquiry', 'ep:edit', 'erp/purchase/inquiry/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3042, '询价单创建', 'erp:purchase-inquiry:create', 3, 1, 3041, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3043, '询价单更新', 'erp:purchase-inquiry:update', 3, 2, 3041, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3044, '询价单删除', 'erp:purchase-inquiry:delete', 3, 3, 3041, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3045, '询价单提交', 'erp:purchase-inquiry:submit', 3, 4, 3041, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3046, '询价单关闭', 'erp:purchase-inquiry:close', 3, 5, 3041, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3047, '报价单', 'erp:purchase-quote:query', 2, 21, 2602, 'purchase-quote', 'ep:tickets', 'erp/purchase/quote/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3048, '报价单创建', 'erp:purchase-quote:create', 3, 1, 3047, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3049, '报价单更新', 'erp:purchase-quote:update', 3, 2, 3047, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3050, '报价单删除', 'erp:purchase-quote:delete', 3, 3, 3047, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3051, '报价单提交', 'erp:purchase-quote:submit', 3, 4, 3047, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3052, '报价单关闭', 'erp:purchase-quote:close', 3, 5, 3047, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3053, '比价单', 'erp:purchase-compare:query', 2, 22, 2602, 'purchase-compare', 'ep:scale-to-original', 'erp/purchase/compare/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3054, '比价单创建', 'erp:purchase-compare:create', 3, 1, 3053, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3055, '比价单更新', 'erp:purchase-compare:update', 3, 2, 3053, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3056, '比价单删除', 'erp:purchase-compare:delete', 3, 3, 3053, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3057, '比价单提交', 'erp:purchase-compare:submit', 3, 4, 3053, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3058, '比价单关闭', 'erp:purchase-compare:close', 3, 5, 3053, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- 四、WMS 越库/计费模块（挂在 WMS 系统 6400 下）
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3059, '越库管理', 'wms:cross-dock-order:query', 2, 30, 6400, 'cross-dock-order', 'ep:right', 'wms/cross-dock-order/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3060, '越库单创建', 'wms:cross-dock-order:create', 3, 1, 3059, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3061, '越库单更新', 'wms:cross-dock-order:update', 3, 2, 3059, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3062, '越库单删除', 'wms:cross-dock-order:delete', 3, 3, 3059, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3063, '计费合同', 'wms:billing-contract:query', 2, 31, 6400, 'billing-contract', 'ep:folder-opened', 'wms/billing-contract/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3064, '计费合同创建', 'wms:billing-contract:create', 3, 1, 3063, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3065, '计费合同更新', 'wms:billing-contract:update', 3, 2, 3063, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3066, '计费合同删除', 'wms:billing-contract:delete', 3, 3, 3063, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3067, '计费账单', 'wms:billing-bill:query', 2, 32, 6400, 'billing-bill', 'ep:document', 'wms/billing-bill/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3068, '计费账单创建', 'wms:billing-bill:create', 3, 1, 3067, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3069, '计费账单更新', 'wms:billing-bill:update', 3, 2, 3067, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3070, '计费账单删除', 'wms:billing-bill:delete', 3, 3, 3067, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- 五、MES 计件/SCADA 模块
--   计件规则、计件记录 挂在 生产管理 5700 下
--   SCADA配置 挂在 设备管理 5300 下
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3071, '计件规则', 'mes:pro-piecework-rule:query', 2, 40, 5700, 'piecework-rule', 'ep:setup', 'mes/pro/piecework-rule/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3072, '计件规则创建', 'mes:pro-piecework-rule:create', 3, 1, 3071, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3073, '计件规则更新', 'mes:pro-piecework-rule:update', 3, 2, 3071, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3074, '计件规则删除', 'mes:pro-piecework-rule:delete', 3, 3, 3071, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3075, '计件记录', 'mes:pro-piecework-record:query', 2, 41, 5700, 'piecework-record', 'ep:list', 'mes/pro/piecework-record/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3076, '计件记录创建', 'mes:pro-piecework-record:create', 3, 1, 3075, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3077, '计件记录更新', 'mes:pro-piecework-record:update', 3, 2, 3075, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3078, '计件记录删除', 'mes:pro-piecework-record:delete', 3, 3, 3075, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3079, 'SCADA配置', 'mes:dv-scada-config:query', 2, 50, 5300, 'scada-config', 'ep:monitor', 'mes/dv/scada-config/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3080, 'SCADA配置创建', 'mes:dv-scada-config:create', 3, 1, 3079, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3081, 'SCADA配置更新', 'mes:dv-scada-config:update', 3, 2, 3079, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3082, 'SCADA配置删除', 'mes:dv-scada-config:delete', 3, 3, 3079, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- 六、CRM 开票/拜访模块（挂在 CRM 系统 2397 下）
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3083, '开票管理', 'crm:invoice:query', 2, 30, 2397, 'invoice', 'ep:Tickets', 'crm/invoice/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3084, '开票管理创建', 'crm:invoice:create', 3, 1, 3083, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3085, '开票管理更新', 'crm:invoice:update', 3, 2, 3083, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3086, '开票管理删除', 'crm:invoice:delete', 3, 3, 3083, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3087, '开票管理提交', 'crm:invoice:submit', 3, 4, 3083, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3088, '拜访签到', 'crm:visit-record:query', 2, 31, 2397, 'visit-record', 'ep:place', 'crm/visit-record/index', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3089, '拜访签到创建', 'crm:visit-record:create', 3, 1, 3088, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3090, '拜访签到更新', 'crm:visit-record:update', 3, 2, 3088, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3091, '拜访签到删除', 'crm:visit-record:delete', 3, 3, 3088, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (3092, '拜访签到提交', 'crm:visit-record:submit', 3, 4, 3088, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
