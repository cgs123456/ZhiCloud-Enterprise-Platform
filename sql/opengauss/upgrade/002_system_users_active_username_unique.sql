-- ============================================================
-- opengauss 存量库升级脚本 002：活跃账号租户级唯一约束
--
-- 背景：
--   sql/opengauss/ 的全新安装脚本已包含 system_users 的 MFA 列，但 MFA 之前
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
--   * 本脚本是静态契约产物，未在任何 opengauss 实库执行过；生产执行前必须
--     在等价实例验证语法、锁等待、索引构建耗时和回滚窗口。
--   * 大小写不敏感唯一性用 lower(username) 逼近 MySQL utf8mb4_unicode_ci
--     语义；重音与尾随空格仍与 MySQL 有差异，属于已知残差，不是等价证明。
--   * 不插入、不修改任何业务数据，不写入任何凭据。
-- ============================================================

DO $$
DECLARE
    v_idx_count integer;
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
    ) THEN
        RAISE EXCEPTION '002 blocked: system_users table missing' USING ERRCODE = '45000';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'tenant_id'
    ) OR NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'username'
    ) OR NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'system_users'
           AND column_name = 'deleted'
    ) THEN
        RAISE EXCEPTION '002 blocked: required columns missing: system_users(tenant_id, username, deleted)' USING ERRCODE = '45000';
    END IF;

    -- 先做数据质量检查再建索引；任何活跃重复都阻断升级，不自动删改业务数据。
    IF EXISTS (
        SELECT 1
          FROM system_users
         WHERE deleted = 0
         GROUP BY tenant_id, lower(username)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '002 blocked: duplicate active username within tenant (case-insensitive)' USING ERRCODE = '45000';
    END IF;

    SELECT COUNT(*) INTO v_idx_count
      FROM pg_indexes
     WHERE schemaname = current_schema()
       AND indexname = 'uk_sysuser_tenant_active';

    IF v_idx_count = 0 THEN
        CREATE UNIQUE INDEX uk_sysuser_tenant_active
            ON system_users (tenant_id, lower(username))
         WHERE deleted = 0;
    ELSIF NOT EXISTS (
        SELECT 1
          FROM pg_indexes
         WHERE schemaname = current_schema()
           AND indexname = 'uk_sysuser_tenant_active'
           AND lower(indexdef) LIKE '%unique%'
           AND lower(indexdef) LIKE '%lower(%'
           AND lower(indexdef) LIKE '%deleted = 0%'
    ) THEN
        RAISE EXCEPTION '002 blocked: uk_sysuser_tenant_active exists but is not the expected partial unique index' USING ERRCODE = '45000';
    END IF;
END $$;

-- 回滚：删除该索引会撤销数据库级并发保护，回到「先查后插」竞态。
-- 回滚前必须确认应用已恢复兼容逻辑，并保留重复账号风险登记与补偿方案。
