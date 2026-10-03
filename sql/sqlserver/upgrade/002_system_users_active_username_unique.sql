-- ============================================================
-- sqlserver 存量库升级脚本 002：活跃账号租户级唯一约束
--
-- 背景：
--   sql/sqlserver/ 的全新安装脚本已包含 system_users 的 MFA 列，但 MFA 之前
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
--   * 本脚本是静态契约产物，未在任何 sqlserver 实库执行过；生产执行前必须
--     在等价实例验证语法、锁等待、索引构建耗时和回滚窗口。
--   * 大小写不敏感唯一性用 lower(username) 逼近 MySQL utf8mb4_unicode_ci
--     语义；重音与尾随空格仍与 MySQL 有差异，属于已知残差，不是等价证明。
--   * 不插入、不修改任何业务数据，不写入任何凭据。
-- ============================================================

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET QUOTED_IDENTIFIER ON;
GO

-- SQL Server 索引键不支持表达式，也不能依赖「唯一索引允许多个 NULL」
-- （SQL Server 唯一索引只接受一个 NULL）。因此这里先加 PERSISTED 计算列
-- active_username，再建带 WHERE active_username IS NOT NULL 的筛选唯一索引，
-- 语义等价于 MySQL V116 的 STORED 生成列。

IF EXISTS (SELECT 1 FROM dbo.system_users WHERE deleted = 0 GROUP BY tenant_id, LOWER(username) HAVING COUNT(*) > 1)
    THROW 50000, N'002 blocked: duplicate active username within tenant (case-insensitive)', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns c
      JOIN sys.tables t ON t.object_id = c.object_id AND t.schema_id = SCHEMA_ID(N'dbo')
     WHERE t.name = N'system_users' AND c.name = N'active_username')
BEGIN
    ALTER TABLE dbo.system_users
        ADD active_username AS (CASE WHEN deleted = 0 THEN LOWER(username) ELSE CONVERT(varchar(30), NULL) END) PERSISTED;
    EXEC sys.sp_addextendedproperty
         @name = N'MS_Description', @value = N'活跃账号名（软删除为 NULL，用于租户级唯一约束）',
         @level0type = N'SCHEMA', @level0name = N'dbo',
         @level1type = N'TABLE',  @level1name = N'system_users',
         @level2type = N'COLUMN', @level2name = N'active_username';
END

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes i
      JOIN sys.tables t ON t.object_id = i.object_id AND t.schema_id = SCHEMA_ID(N'dbo')
     WHERE t.name = N'system_users' AND i.name = N'uk_sysuser_tenant_active')
BEGIN
    CREATE UNIQUE INDEX uk_sysuser_tenant_active ON dbo.system_users (tenant_id, active_username)
        WHERE active_username IS NOT NULL;
END
ELSE IF EXISTS (
    SELECT 1 FROM sys.indexes i
      JOIN sys.tables t ON t.object_id = i.object_id AND t.schema_id = SCHEMA_ID(N'dbo')
     WHERE t.name = N'system_users' AND i.name = N'uk_sysuser_tenant_active'
       AND (i.is_unique <> 1 OR i.has_filter <> 1))
    THROW 50000, N'002 blocked: uk_sysuser_tenant_active exists but is not the expected filtered unique index', 1;

-- 回滚：删除该索引和计算列会撤销数据库级并发保护，回到「先查后插」竞态。
-- 回滚前必须确认应用已恢复兼容逻辑，并保留重复账号风险登记与补偿方案。
