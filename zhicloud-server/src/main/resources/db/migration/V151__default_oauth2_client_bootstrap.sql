-- ============================================================
-- V151：补齐 `default` OAuth2 客户端（管理员登录基础设施）
-- ============================================================
-- 背景（2026-10-09 真机证据：隔离 MySQL 8.4.11 + 后端 48080 容器 E2E）：
--   AdminAuthServiceImpl.createTokenAfterLoginSuccess() 硬编码调用
--   oauth2TokenService.createAccessToken(userId, userType, "default", null)，
--   而 V1.1 的 system_oauth2_client 记录区在 63f99a812（drop V1.1 seed
--   credentials）后已被清空 → 全新库 system_oauth2_client 为空，
--   任何账号密码登录都返回 code=1002020000 "OAuth2 客户端不存在"。
--   README/首次安装因此无法登录，属 Issue #1 的实质残留。
--
-- 设计取舍：
--   * `default` 是**登录基础设施**，不是演示客户端。V115 把它与 test /
--     zhicloud-sso-demo-* 演示客户端一并禁用，是把它误当演示客户端处理；
--     V115 版本号小于 151，本迁移在它之后纠正 default 的状态。
--   * 不写死 Secret：内部登录/刷新路径 validOAuthClientFromCache(clientId)
--     只校验"客户端存在且启用"，从不校验 Secret；因此 Secret 用
--     SHA2(UUID(), 256) 在**执行期**随机生成，源码 / Git 历史 / 迁移字节里
--     都不出现可用凭据（不触发安全基线 R6 / R10）。
--   * 授权类型刻意**不含 client_credentials**：validOAuthClientFromCache 对
--     空 client_secret 直接跳过校验（StrUtil.isNotEmpty(clientSecret)），
--     保留 client_credentials 会新开一条 `Basic default:` 匿名换取令牌的路径。
--     管理员登录与刷新都不检查授权类型，故去掉它不影响登录链路；
--     如确有外部集成需要，可在客户端管理界面按需加回。
--   * 存量库（曾装过 63f99a812 之前的 V1.1）：V115 已把同名客户端 status 置 1，
--     本迁移把它重新启用，并把历史公开 Secret 轮换成执行期随机值。
--   * 不动 V1.1 历史字节（已发布迁移，改字节会触发 Flyway 校验和失败）。
--   * 幂等：先按 client_id='default' AND deleted=b'0' 预检；UPDATE 仅在
--     status<>0 时匹配，重跑为 no-op。
-- ============================================================

-- 1) 不存在则插入（Secret 由数据库随机生成，不落源码）
INSERT INTO `system_oauth2_client`
(`client_id`, `secret`, `name`, `logo`, `description`, `status`,
 `access_token_validity_seconds`, `refresh_token_validity_seconds`,
 `redirect_uris`, `authorized_grant_types`, `scopes`, `auto_approve_scopes`,
 `authorities`, `resource_ids`, `additional_information`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'default', SHA2(UUID(), 256), '智云', '', '管理员登录内部客户端（非演示客户端）', 0,
       1800, 2592000,
       '["https://www.zhicloud.cn","https://doc.zhicloud.cn"]',
       '["password","authorization_code","implicit","refresh_token"]',
       '["user.read","user.write"]', '[]',
       '["user.read","user.write"]', '[]', '{}',
       'system', NOW(), 'system', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_oauth2_client` e
     WHERE e.`client_id` = 'default' AND e.`deleted` = b'0'
);

-- 2) 存量库修复：被 V115 禁用的同名客户端重新启用，并轮换历史公开 Secret。
--    仅当 status<>0 时匹配，重复执行为 no-op（不会反复改写 Secret）。
UPDATE `system_oauth2_client`
   SET `status` = 0,
       `secret` = SHA2(UUID(), 256),
       `updater` = 'system',
       `update_time` = NOW()
 WHERE `client_id` = 'default'
   AND `deleted` = b'0'
   AND `status` <> 0;

-- 3) 自检 1：default 客户端存在且启用、Secret 非空、TTL 齐全
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_oauth2_client`
                   WHERE `client_id` = 'default' AND `deleted` = b'0'
                     AND `status` = 0 AND `secret` <> ''
                     AND `access_token_validity_seconds` > 0
                     AND `refresh_token_validity_seconds` > 0) = 1
            THEN 'V151_DEFAULT_CLIENT_READY PASS'
            ELSE 'V151_DEFAULT_CLIENT_READY FAIL' END AS check_result;

-- 4) 自检 2：default 行唯一（deleted=0），防止插入出重复客户端
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_oauth2_client`
                   WHERE `client_id` = 'default' AND `deleted` = b'0') = 1
            THEN 'V151_DEFAULT_CLIENT_UNIQUE PASS'
            ELSE CONCAT('V151_DEFAULT_CLIENT_UNIQUE FAIL ',
                        (SELECT COUNT(*) FROM `system_oauth2_client`
                          WHERE `client_id` = 'default' AND `deleted` = b'0')) END AS check_result;
