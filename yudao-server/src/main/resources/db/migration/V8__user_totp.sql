-- ============================================================
-- V8: 用户 TOTP 双因素认证字段
--
-- 在 system_users 表新增两个字段：
--   totp_secret   TOTP 密钥（AES 加密存储，Base64）
--   totp_enabled  是否启用 TOTP 双因素认证
--
-- 兼容性：纯增量字段，默认值不破坏历史数据
-- ============================================================

ALTER TABLE `system_users`
    ADD COLUMN `totp_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'TOTP 密钥（加密存储）' AFTER `login_date`,
    ADD COLUMN `totp_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用 TOTP 双因素认证（0未启用 1已启用）' AFTER `totp_secret`;
