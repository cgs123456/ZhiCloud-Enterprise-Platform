-- ============================================================
-- oracle 存量库升级脚本 002：活跃账号租户级唯一约束
--
-- 背景：
--   sql/oracle/ 的全新安装脚本已包含 system_users 的 MFA 列，但 MFA 之前
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
--   * 本脚本是静态契约产物，未在任何 oracle 实库执行过；生产执行前必须
--     在等价实例验证语法、锁等待、索引构建耗时和回滚窗口。
--   * 大小写不敏感唯一性用 lower(username) 逼近 MySQL utf8mb4_unicode_ci
--     语义；重音与尾随空格仍与 MySQL 有差异，属于已知残差，不是等价证明。
--   * 不插入、不修改任何业务数据，不写入任何凭据。
-- ============================================================

-- Oracle 没有部分索引；这里用函数索引：deleted = 0 时索引 lower(username)，
-- 否则索引 NULL。Oracle 不索引全 NULL 的表达式结果，因此软删除行不参与
-- 唯一约束，语义与 MySQL V116 的 STORED 生成列一致。
DECLARE
    v_cnt  NUMBER;
    v_dup  NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM USER_TABLES WHERE TABLE_NAME = 'SYSTEM_USERS';
    IF v_cnt = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, '002 blocked: system_users table missing');
    END IF;

    SELECT COUNT(*) INTO v_cnt
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'SYSTEM_USERS'
       AND COLUMN_NAME IN ('TENANT_ID', 'USERNAME', 'DELETED');
    IF v_cnt <> 3 THEN
        RAISE_APPLICATION_ERROR(-20002, '002 blocked: required columns missing: system_users(tenant_id, username, deleted)');
    END IF;

    -- 先做数据质量检查再建索引；任何活跃重复都阻断升级，不自动删改业务数据。
    SELECT COUNT(*) INTO v_dup
      FROM (SELECT 1
              FROM system_users
             WHERE deleted = 0
             GROUP BY tenant_id, LOWER(username)
            HAVING COUNT(*) > 1);
    IF v_dup > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, '002 blocked: duplicate active username within tenant (case-insensitive)');
    END IF;

    SELECT COUNT(*) INTO v_cnt FROM USER_INDEXES WHERE INDEX_NAME = 'UK_SYSUSER_TENANT_ACTIVE';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_sysuser_tenant_active ON system_users (tenant_id, CASE WHEN deleted = 0 THEN LOWER(username) ELSE NULL END)';
    ELSE
        SELECT COUNT(*) INTO v_cnt
          FROM USER_INDEXES
         WHERE INDEX_NAME = 'UK_SYSUSER_TENANT_ACTIVE'
           AND UNIQUENESS = 'UNIQUE';
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(-20004, '002 blocked: uk_sysuser_tenant_active exists but is not UNIQUE');
        END IF;
        -- 索引表达式细节存在 USER_IND_EXPRESSIONS.COLUMN_EXPRESSION（LONG 类型，
        -- 无法在 PL/SQL 中做 LIKE 比较），因此这里只校验唯一性；表达式是否符合
        -- 预期必须由 DBA 在实库人工复核，不得据此宣称等价。
    END IF;
END;
/

-- 回滚：删除该索引会撤销数据库级并发保护，回到「先查后插」竞态。
-- 回滚前必须确认应用已恢复兼容逻辑，并保留重复账号风险登记与补偿方案。
