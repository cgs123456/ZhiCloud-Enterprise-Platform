-- ============================================================
-- V8: 用户 TOTP 双因素认证字段
--
-- 在 system_users 表新增两个字段：
--   totp_secret   TOTP 密钥（AES 加密存储，Base64）
--   totp_enabled  是否启用 TOTP 双因素认证
--
-- 兼容性：纯增量字段，默认值不破坏历史数据
--
-- 幂等性：全新库初始化时，主脚本 sql/mysql/zhicloud_platform.sql 的 system_users
--         可能已包含这两个列，直接 ADD COLUMN 会报 Duplicate column。
--         故改用 information_schema 判断列是否存在，缺失时才执行 ALTER，
--         保证「全新库（列已存在）」与「存量库（列缺失）」两种场景均可重复执行。
-- ============================================================

-- 1) totp_secret 列
SET @totp_secret_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_users'
      AND column_name = 'totp_secret'
);
SET @totp_secret_ddl = IF(@totp_secret_exists = 0,
    'ALTER TABLE `system_users` ADD COLUMN `totp_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''TOTP 密钥（加密存储）'' AFTER `login_date`',
    'SELECT 1');
PREPARE totp_secret_stmt FROM @totp_secret_ddl;
EXECUTE totp_secret_stmt;
DEALLOCATE PREPARE totp_secret_stmt;

-- 2) totp_enabled 列
SET @totp_enabled_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_users'
      AND column_name = 'totp_enabled'
);
SET @totp_enabled_ddl = IF(@totp_enabled_exists = 0,
    'ALTER TABLE `system_users` ADD COLUMN `totp_enabled` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否启用 TOTP 双因素认证（0未启用 1已启用）'' AFTER `totp_secret`',
    'SELECT 1');
PREPARE totp_enabled_stmt FROM @totp_enabled_ddl;
EXECUTE totp_enabled_stmt;
DEALLOCATE PREPARE totp_enabled_stmt;
