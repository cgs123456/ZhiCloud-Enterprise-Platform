-- MES 物料库存唯一索引（防止并发插入竞态）
-- 为 mes_wm_material_stock 添加 (item_id, warehouse_id, location_id, area_id, batch_id) 唯一索引
-- 确保同一物料在同一仓库库区库位的同一批次只有一条库存记录

-- 幂等新增唯一索引（MySQL 8.0+）
ALTER TABLE `mes_wm_material_stock`
ADD UNIQUE INDEX `uk_item_warehouse_location_area_batch` (`item_id`, `warehouse_id`, `location_id`, `area_id`, `batch_id`)
ON DUPLICATE KEY UPDATE `id` = `id`;
