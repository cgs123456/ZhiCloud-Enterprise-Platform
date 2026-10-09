-- V151 验证：default OAuth2 客户端（管理员登录基础设施）的行为断言
-- 背景：AdminAuthServiceImpl 硬编码 CLIENT_ID_DEFAULT='default'，缺行即登录失败。
-- 本文件断言迁移后 default 客户端可用，且没有把匿名 client_credentials 路径带回来。

-- 1. Flyway 通道：V151 已成功应用
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '151' AND success = 1) = 1
            THEN 'V151_FLYWAY_APPLIED PASS'
            ELSE 'V151_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. default 客户端存在、启用、Secret 非空、TTL 齐全（缺一即登录失败）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_oauth2_client
                   WHERE client_id = 'default' AND deleted = b'0'
                     AND status = 0 AND secret IS NOT NULL AND secret <> ''
                     AND access_token_validity_seconds > 0
                     AND refresh_token_validity_seconds > 0) = 1
            THEN 'V151_DEFAULT_CLIENT_USABLE PASS'
            ELSE 'V151_DEFAULT_CLIENT_USABLE FAIL' END AS check_result;

-- 3. default 行唯一（deleted=0）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_oauth2_client
                   WHERE client_id = 'default' AND deleted = b'0') = 1
            THEN 'V151_DEFAULT_CLIENT_UNIQUE PASS'
            ELSE 'V151_DEFAULT_CLIENT_UNIQUE FAIL' END AS check_result;

-- 4. 授权类型不含 client_credentials（否则空 Secret 会绕过校验、匿名换令牌）
SELECT CASE WHEN JSON_CONTAINS(
                   (SELECT authorized_grant_types FROM system_oauth2_client
                     WHERE client_id = 'default' AND deleted = b'0' LIMIT 1),
                   '"client_credentials"') = 0
            THEN 'V151_NO_ANONYMOUS_GRANT PASS'
            ELSE 'V151_NO_ANONYMOUS_GRANT FAIL' END AS check_result;

-- 5. Secret 不是历史公开值（轮换生效，且未把公开凭据写回源码/库）
SELECT CASE WHEN (SELECT secret FROM system_oauth2_client
                   WHERE client_id = 'default' AND deleted = b'0' LIMIT 1)
                 NOT IN ('admin123', 'test', 'test2')
            THEN 'V151_SECRET_ROTATED PASS'
            ELSE 'V151_SECRET_ROTATED FAIL' END AS check_result;
