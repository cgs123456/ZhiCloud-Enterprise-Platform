-- ============================================================
-- kingbase 存量库升级脚本 001：system_users MFA 列
--
-- 背景：
--   sql/kingbase/ 的全新安装脚本已包含 system_users 的 MFA 列，但 MFA 之前
--   创建的存量库没有这两列；同时非 MySQL 全新安装脚本缺少与 MySQL Flyway
--   V116 等价的「活跃账号租户级数据库唯一约束」。MySQL 侧由 V115/V116/V117
--   覆盖，非 MySQL 侧走本目录的手工升级脚本，顺序必须是 001 -> 002。
--
-- 目标：
--   001：幂等补齐 totp_secret / totp_enabled；定义不兼容时 fail-closed。
--   002：对同一 tenant_id 下未删除的 username 建立数据库唯一性，软删除行
--        不参与约束，存量活跃重复账号 fail-closed，不自动删改业务数据。
--
-- 边界：
--   * 本脚本是静态契约产物，未在任何 kingbase 实库执行过；生产执行前必须
--     在等价实例验证语法、锁等待、索引构建耗时和回滚窗口。
--   * 大小写不敏感唯一性用 lower(username) 逼近 MySQL utf8mb4_unicode_ci
--     语义；重音与尾随空格仍与 MySQL 有差异，属于已知残差，不是等价证明。
--   * 不插入、不修改任何业务数据，不写入任何凭据。
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
    ) THEN
        RAISE EXCEPTION '001 blocked: system_users table missing' USING ERRCODE = '45000';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'totp_secret'
    ) THEN
        ALTER TABLE system_users ADD COLUMN totp_secret varchar(255) NULL;
        COMMENT ON COLUMN system_users.totp_secret IS 'TOTP 密钥（加密存储）';
    ELSIF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'totp_secret'
           AND data_type = 'character varying'
           AND character_maximum_length = 255
           AND is_nullable = 'YES'
    ) THEN
        RAISE EXCEPTION '001 blocked: totp_secret exists but has an incompatible definition' USING ERRCODE = '45000';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'totp_enabled'
    ) THEN
        ALTER TABLE system_users ADD COLUMN totp_enabled smallint NOT NULL DEFAULT 0;
        COMMENT ON COLUMN system_users.totp_enabled IS '是否启用 TOTP 双因素认证（0未启用 1已启用）';
    ELSIF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'totp_enabled'
           AND data_type = 'smallint'
           AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION '001 blocked: totp_enabled exists but has an incompatible definition' USING ERRCODE = '45000';
    END IF;
END $$;

-- 回滚：删除两列会撤销 MFA 能力并使已启用 TOTP 的账号无法登录。
-- 回滚前必须确认无账号处于 totp_enabled = 1，并保留双因素降级方案。
