-- ****************************************************************************
-- P2 工程纪律：ERP 库存聚合根乐观锁版本列
-- 与 WMS(Q76 之前)/QMS/CRM/MES 的 @Version 收口保持一致。
-- 说明：ERP 库存变更热路径使用自定义 CAS（updateCountIncrement / updateLockedCountIncrement），
-- 不经过 MyBatis-Plus updateById，故 @Version 当前为防御性兜底；该列存在不影响既有 CAS 语义。
-- 幂等：通过 information_schema 判断列是否存在，避免重复执行报错。
-- ****************************************************************************
SET @exist_erp_stock_version = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'erp_stock' AND column_name = 'version');

SET @sql_erp_stock_version = IF(@exist_erp_stock_version = 0,
    'ALTER TABLE erp_stock ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号（P2 @Version）''',
    'SELECT 1');

PREPARE stmt_erp_stock_version FROM @sql_erp_stock_version;
EXECUTE stmt_erp_stock_version;
DEALLOCATE PREPARE stmt_erp_stock_version;
