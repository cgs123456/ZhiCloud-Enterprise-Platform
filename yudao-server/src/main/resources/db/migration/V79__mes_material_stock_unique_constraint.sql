-- ============================================================
-- V79: MES 物料库存唯一索引（防止并发插入竞态）
--
-- 为 mes_wm_material_stock 添加
--   (item_id, warehouse_id, location_id, area_id, batch_id)
-- 唯一索引，确保同一物料在同一仓库/库区/库位的同一批次只有一条库存记录。
--
-- 原实现的错误：
--   ALTER TABLE ... ADD UNIQUE INDEX ... ON DUPLICATE KEY UPDATE `id` = `id`;
--   `ON DUPLICATE KEY UPDATE` 只对 INSERT 语句有效，附加在 ALTER TABLE 上是
--   纯语法错误（ERROR 1064），该迁移从未真正生效过。
--
-- 正确做法：查 information_schema.statistics 判断索引是否已存在，
--          仅在缺失时执行 ALTER，实现幂等（存量库/全新库均安全）。
--
-- 已知语义限制：上述 5 个列在表定义中均可为 NULL，而 MySQL 的唯一索引
--   把每个 NULL 视为互不相等，因此当 location_id / area_id / batch_id 为 NULL 时
--   该索引无法阻止重复行。业务侧的库存写入必须保证这些维度非空，
--   或在 Service 层配合乐观锁/唯一键兜底。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v79_add_material_stock_uk;

DELIMITER $$
CREATE PROCEDURE p_v79_add_material_stock_uk()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_wm_material_stock'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_wm_material_stock'
          AND index_name = 'uk_item_warehouse_location_area_batch'
    ) THEN
        ALTER TABLE `mes_wm_material_stock`
            ADD UNIQUE INDEX `uk_item_warehouse_location_area_batch`
            (`item_id`, `warehouse_id`, `location_id`, `area_id`, `batch_id`);
    END IF;
END$$
DELIMITER ;

CALL p_v79_add_material_stock_uk();

DROP PROCEDURE IF EXISTS p_v79_add_material_stock_uk;
