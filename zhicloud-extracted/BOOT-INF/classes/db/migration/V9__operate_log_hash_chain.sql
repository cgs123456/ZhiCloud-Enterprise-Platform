-- ============================================================
-- V9: 操作日志 Hash 链式审计字段
--
-- 在 system_operate_log 表新增两个字段：
--   prev_hash     前一条日志的 hash（链上的第一条日志该字段为空字符串）
--   current_hash  本条日志的 hash = SHA256(prevHash + type + subType + bizId + userId + requestUrl + requestMethod + action + createTime)
--
-- 兼容性：纯增量字段，默认值 '' 不破坏历史数据
-- ============================================================

ALTER TABLE `system_operate_log`
    ADD COLUMN `prev_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '前一条日志的 hash（Hash 链式审计）' AFTER `user_agent`,
    ADD COLUMN `current_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '本条日志的 hash（Hash 链式审计）' AFTER `prev_hash`;
