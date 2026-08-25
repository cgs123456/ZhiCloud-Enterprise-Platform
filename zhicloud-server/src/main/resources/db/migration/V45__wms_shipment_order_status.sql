-- ============================================================
-- V45: WMS 出库单状态机细分（P0-3）
--
-- 背景：原 WmsOrderStatusEnum 仅 3 状态（PREPARE=0 / FINISHED=4 / CANCELED=5）
-- 新增 5 个中间状态（PICKING/PICKED/REVIEWED/PACKED/SHIPPED），并将
--   - FINISHED 由历史值 4 迁移到 99
--   - CANCELED 由历史值 5 迁移到 -1
-- 以便给中间状态留出 10/20/30/40/50 的连续编码空间。
--
-- 影响表（共享 WmsOrderStatusEnum 的所有单据表）：
--   wms_shipment_order
--   wms_receipt_order
--   wms_check_order
--   wms_wave_order
--   wms_movement_order
--
-- 兼容性：
--   - PREPARE=0 保持不变，草稿状态数据无需迁移
--   - 历史值为 4（FINISHED）→ 99
--   - 历史值为 5（CANCELED）→ -1
--   - status 列类型为 TINYINT（signed），-1 在取值范围内
--
-- 幂等性：使用 WHERE status IN (4, 5) 条件，重复执行不会二次迁移
-- ============================================================

-- ----------------------------
-- 1. wms_shipment_order
-- ----------------------------
UPDATE wms_shipment_order SET status = 99 WHERE status = 4 AND deleted = 0;
UPDATE wms_shipment_order SET status = -1 WHERE status = 5 AND deleted = 0;

-- ----------------------------
-- 2. wms_receipt_order
-- ----------------------------
UPDATE wms_receipt_order SET status = 99 WHERE status = 4 AND deleted = 0;
UPDATE wms_receipt_order SET status = -1 WHERE status = 5 AND deleted = 0;

-- ----------------------------
-- 3. wms_check_order
-- ----------------------------
UPDATE wms_check_order SET status = 99 WHERE status = 4 AND deleted = 0;
UPDATE wms_check_order SET status = -1 WHERE status = 5 AND deleted = 0;

-- ----------------------------
-- 4. wms_wave_order
-- ----------------------------
UPDATE wms_wave_order SET status = 99 WHERE status = 4 AND deleted = 0;
UPDATE wms_wave_order SET status = -1 WHERE status = 5 AND deleted = 0;

-- ----------------------------
-- 5. wms_movement_order
-- ----------------------------
UPDATE wms_movement_order SET status = 99 WHERE status = 4 AND deleted = 0;
UPDATE wms_movement_order SET status = -1 WHERE status = 5 AND deleted = 0;

-- ----------------------------
-- 6. 修正 wms_shipment_order.status 列 COMMENT（仅注释更新，类型不变）
-- ----------------------------
ALTER TABLE wms_shipment_order
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
    COMMENT '状态（-1 已作废 0 草稿 10 拣货中 20 已拣货 30 已复核 40 已打包 50 已发货 99 已完成）';
