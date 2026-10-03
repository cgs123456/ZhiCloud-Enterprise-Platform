-- ============================================================
-- oracle 存量库升级脚本 001：system_users MFA 列
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

DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM USER_TABLES WHERE TABLE_NAME = 'SYSTEM_USERS';
    IF v_cnt = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, '001 blocked: system_users table missing');
    END IF;

    SELECT COUNT(*) INTO v_cnt
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'SYSTEM_USERS' AND COLUMN_NAME = 'TOTP_SECRET';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE system_users ADD (totp_secret varchar2(255))';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN system_users.totp_secret IS ''TOTP 密钥（加密存储）''';
    ELSE
        SELECT COUNT(*) INTO v_cnt
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME = 'SYSTEM_USERS'
           AND COLUMN_NAME = 'TOTP_SECRET'
           AND DATA_TYPE = 'VARCHAR2'
           AND DATA_LENGTH = 255
           AND NULLABLE = 'Y';
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(-20002, '001 blocked: totp_secret exists but has an incompatible definition');
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_cnt
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'SYSTEM_USERS' AND COLUMN_NAME = 'TOTP_ENABLED';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE system_users ADD (totp_enabled number(1,0) DEFAULT 0 NOT NULL)';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN system_users.totp_enabled IS ''是否启用 TOTP 双因素认证（0未启用 1已启用）''';
    ELSE
        SELECT COUNT(*) INTO v_cnt
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME = 'SYSTEM_USERS'
           AND COLUMN_NAME = 'TOTP_ENABLED'
           AND DATA_TYPE = 'NUMBER'
           AND DATA_PRECISION = 1
           AND DATA_SCALE = 0
           AND NULLABLE = 'N';
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(-20003, '001 blocked: totp_enabled exists but has an incompatible definition');
        END IF;
    END IF;
END;
/

-- 回滚：删除两列会撤销 MFA 能力并使已启用 TOTP 的账号无法登录。
-- 回滚前必须确认无账号处于 totp_enabled = 1，并保留双因素降级方案。
