-- ============================================================
-- V116: system_users 活跃账号租户级数据库唯一约束
--
-- 背景：
--   system_users 只有 username 普通索引；应用层 validateUsernameUnique /
--   selectByUsername 的“先查后插”无法阻止并发请求创建同一租户下的重复账号。
--
-- 目标：
--   1. 对同一 tenant_id 下未删除（deleted = 0）的 username 建立数据库唯一性；
--   2. 保留逻辑删除历史行，不阻塞账号删除后合法重建；
--   3. 迁移可重复执行；发现存量活跃重复时 fail-closed，不自动删改业务数据。
--
-- 实现：
--   active_username 是 STORED 生成列：
--     deleted = 0  -> username
--     deleted <> 0 -> NULL
--   MySQL 唯一索引允许多个 NULL，因此软删除行不参与活跃账号唯一性约束。
--
-- 边界：
--   * 这是 MySQL 迁移。其他数据库方言的并发唯一性和迁移脚本必须单独实现并实库验证。
--   * ADD COLUMN / ADD UNIQUE KEY 在存量大表上可能触发表重建或长时间 MDL；
--     生产执行前必须评估备份、磁盘空间、复制延迟和低峰窗口。
--   * 迁移不清理重复数据；发现重复必须先人工合并/改名并保留审计证据。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v116_system_users_active_username_unique;

DELIMITER $$
CREATE PROCEDURE p_v116_system_users_active_username_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V116 table missing: system_users';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = 'tenant_id'
    ) OR NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = 'username'
    ) OR NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = 'deleted'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V116 required columns missing: system_users(tenant_id, username, deleted)';
    END IF;

    -- 先于 DDL 检查存量数据；发现重复直接失败，不自动删除/改名。
    IF EXISTS (
        SELECT 1
          FROM system_users
         WHERE deleted = 0
         GROUP BY tenant_id, username
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V116 blocked: duplicate active username within tenant';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = 'active_username'
    ) THEN
        ALTER TABLE `system_users`
            ADD COLUMN `active_username` varchar(30)
                CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                GENERATED ALWAYS AS (
                    CASE WHEN `deleted` = 0 THEN `username` ELSE NULL END
                ) STORED
                COMMENT '活跃账号名（软删除为 NULL，用于租户级唯一约束）';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'system_users'
               AND column_name = 'active_username'
               AND data_type = 'varchar'
               AND character_maximum_length = 30
               AND extra LIKE '%STORED GENERATED%'
               AND generation_expression IS NOT NULL
               AND LOWER(
                   REPLACE(
                     REPLACE(
                       REPLACE(
                         REPLACE(
                           REPLACE(
                             REPLACE(
                               REPLACE(generation_expression, '`', ''),
                               ' ', ''),
                             CHAR(9), ''),
                           CHAR(10), ''),
                         CHAR(13), ''),
                       '(', ''),
                     ')', '')
               ) = 'casewhendeleted=0thenusernameelsenullend'
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V116 blocked: active_username exists but is not the expected STORED generated column';
        END IF;
    END IF;

    -- 防止部分升级或人工改动后生成列值与活跃账号语义不一致。
    IF EXISTS (
        SELECT 1
          FROM system_users
         WHERE deleted = 0
           AND active_username IS NOT NULL
         GROUP BY tenant_id, active_username
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V116 blocked: duplicate generated active_username within tenant';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND index_name = 'uk_system_users_tenant_active_username'
    ) THEN
        ALTER TABLE `system_users`
            ADD UNIQUE KEY `uk_system_users_tenant_active_username` (`tenant_id`, `active_username`);
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = 'system_users'
               AND index_name = 'uk_system_users_tenant_active_username'
             GROUP BY table_name, index_name
            HAVING COUNT(*) = 2
               AND MAX(non_unique) = 0
               AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN column_name = 'active_username' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 2
               AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 2
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V116 blocked: active username unique index has unexpected definition';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL p_v116_system_users_active_username_unique();
DROP PROCEDURE IF EXISTS p_v116_system_users_active_username_unique;

-- 回滚：删除唯一索引和生成列会撤销数据库级并发保护。回滚前必须确认应用已
-- 恢复兼容逻辑，并保留重复账号风险登记与补偿方案；不能单独回滚本迁移。
