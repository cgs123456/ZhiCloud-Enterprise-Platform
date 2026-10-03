-- ============================================================
-- V115: 生产种子账号清理与演示客户端禁用
--
-- 目标：
--   1. 仅按 (id, username, password_hash, tenant_id) 精确匹配 V1.1 中的
--      20 个公开种子账号，避免按用户名误伤同名真实账号。
--   2. 保留 system_users 历史行，使用 status=1、deleted=b'1' 软禁用。
--   3. 撤销种子账号的授权关系、岗位关系、OAuth2 会话和社交绑定。
--   4. 禁用 V1.1 内置 OAuth2/社交演示客户端，但不写入或替换任何 Secret。
--
-- 安全门禁：
--   如果当前库仍存在启用的、精确匹配 V1.1 的种子账号，且不存在一个启用的、
--   非种子 super_admin，迁移直接失败。这样可避免清理掉最后一个管理员后把
--   系统锁死，同时允许 clean baseline / 已清理库安全 no-op。
--   首次初始化必须先通过 scripts/production/init_bootstrap_admin.py 创建独立
--   管理员，再执行本迁移。
--
-- 边界：
--   * 本迁移不会把 V1.1 中已发布的历史 Secret 从源码删除；源码中仍会保留
--     公开种子文本。生产部署必须同时执行 Secret 轮换和旧凭据失效流程。
--   * 演示客户端仅被禁用，历史行保留用于审计。
--   * 本迁移可重复执行；已软禁用的种子账号不会被物理删除。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v115_production_seed_cleanup;
DROP TEMPORARY TABLE IF EXISTS tmp_v115_seed_users;

CREATE TEMPORARY TABLE tmp_v115_seed_users (
    id BIGINT NOT NULL,
    username VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    tenant_id BIGINT NOT NULL,
    PRIMARY KEY (id, tenant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_v115_seed_users (id, username, password_hash, tenant_id) VALUES
(1, 'admin', '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG', 1),
(100, 'zhicloud', '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG', 1),
(103, 'yuanma', '$2a$04$k/d6mc0nySN0i2udwcI8Ee8V5aM5OHixBRbQfXmPuFTUl3Zf/DBs.', 1),
(104, 'test', '$2a$04$BrwaYn303hjA/6TnXqdGoOLhyHOAA0bVrAFu6.1dJKycqKUnIoRz2', 1),
(107, 'admin107', '$2a$10$dYOOBKMO93v/.ReCqzyFg.o67Tqk.bbc2bhrpyBGkIw9aypCtr2pm', 118),
(108, 'admin108', '$2a$10$y6mfvKoNYL1GXWak8nYwVOH.kCWqjactkzdoIDgiKl93WN3Ejg.Lu', 119),
(109, 'admin109', '$2a$10$JAqvH0tEc0I7dfDVBI7zyuB4E3j.uH6daIjV53.vUS6PknFkDJkuK', 120),
(110, 'admin110', '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm', 121),
(111, 'test', '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm', 121),
(112, 'newobject', '$2a$04$dB0z8Q819fJWz0hbaLe6B.VfHCjYgWx6LFfET5lyz3JwcqlyCkQ4C', 1),
(113, 'aoteman', '$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO', 122),
(114, 'hrmgr', '$2a$10$TR4eybBioGRhBmDBWkqWLO6NIh3mzYa8KBKDDB5woiGYFVlRAi.fu', 1),
(115, 'aotemane', '$2a$04$GcyP0Vyzb2F2Yni5PuIK9ueGxM0tkZGMtDwVRwrNbtMvorzbpNsV2', 1),
(117, 'admin123', '$2a$04$sEtimsHu9YCkYY4/oqElHem2Ijc9ld20eYO6lN.g/21NfLUTDLB9W', 1),
(118, 'goudan', '$2a$04$3suGZjnA6rM5bErf38u1felbgqbsPHGdRG3l9NkxPCEt2ah9Y6aJi', 1),
(139, 'wwbwwb', '$2a$04$FJLIyg8lbPytP29pbZaiU.LesJvCsYfEaHqQfB0pGQhK3e9BeZmLy', 1),
(141, 'admin1', '$2a$04$oj6F6d7HrZ70kYVD3TNzEu.m3TPUzajOVuC66zdKna8KRerK1FmVa', 1),
(142, 'test01', '$2a$04$4bCYWZkjxxOC4QE0LY2M9uEEKWeJbLfs489NFtQoyidL5I0FndRaO', 1),
(143, 'a00001', '$2a$04$GhVHFviOw/SsTmiQtifHJesDYFlHMeGK7OWh7aGCCjGGVCmbHVAwa', 1),
(144, 'aoteman001', '$2a$04$omQOmhz8OyUFBKw77nr8KOtMp6xdvoQ1gWStjk9r8.OYT3Bv6oEYe', 1);

DELIMITER $$
CREATE PROCEDURE p_v115_production_seed_cleanup()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM system_users u
          JOIN tmp_v115_seed_users s
            ON s.id = u.id
           AND s.username = u.username
           AND s.password_hash = u.password
           AND s.tenant_id = u.tenant_id
         WHERE u.status = 0
           AND u.deleted = b'0'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM system_users u
              JOIN system_user_role ur
                ON ur.user_id = u.id
               AND ur.tenant_id = u.tenant_id
               AND ur.deleted = b'0'
              JOIN system_role r
                ON r.id = ur.role_id
               AND r.tenant_id = u.tenant_id
               AND r.deleted = b'0'
              LEFT JOIN tmp_v115_seed_users s
                ON s.id = u.id
               AND s.username = u.username
               AND s.password_hash = u.password
               AND s.tenant_id = u.tenant_id
             WHERE u.status = 0
               AND u.deleted = b'0'
               AND r.code = 'super_admin'
               AND r.status = 0
               AND s.id IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V115 blocked: non-seed super_admin required before seed cleanup';
        END IF;
    END IF;

    UPDATE `system_user_role` ur
      JOIN tmp_v115_seed_users s
        ON s.id = ur.user_id
       AND s.tenant_id = ur.tenant_id
       SET ur.deleted = b'1',
           ur.update_time = NOW()
     WHERE ur.deleted = b'0';

    UPDATE `system_user_post` up
      JOIN tmp_v115_seed_users s
        ON s.id = up.user_id
       AND s.tenant_id = up.tenant_id
       SET up.deleted = b'1',
           up.update_time = NOW()
     WHERE up.deleted = b'0';

    UPDATE `system_oauth2_access_token` token
      JOIN tmp_v115_seed_users s
        ON s.id = token.user_id
       AND s.tenant_id = token.tenant_id
       SET token.deleted = b'1',
           token.expires_time = NOW(),
           token.update_time = NOW()
     WHERE token.deleted = b'0';

    UPDATE `system_oauth2_refresh_token` token
      JOIN tmp_v115_seed_users s
        ON s.id = token.user_id
       AND s.tenant_id = token.tenant_id
       SET token.deleted = b'1',
           token.expires_time = NOW(),
           token.update_time = NOW()
     WHERE token.deleted = b'0';

    UPDATE `system_oauth2_approve` approve
      JOIN tmp_v115_seed_users s
        ON s.id = approve.user_id
       AND s.tenant_id = approve.tenant_id
       SET approve.deleted = b'1',
           approve.expires_time = NOW(),
           approve.update_time = NOW()
     WHERE approve.deleted = b'0';

    UPDATE `system_oauth2_code` code
      JOIN tmp_v115_seed_users s
        ON s.id = code.user_id
       AND s.tenant_id = code.tenant_id
       SET code.deleted = b'1',
           code.expires_time = NOW(),
           code.update_time = NOW()
     WHERE code.deleted = b'0';

    UPDATE `system_social_user_bind` bind
      JOIN tmp_v115_seed_users s
        ON s.id = bind.user_id
       AND s.tenant_id = bind.tenant_id
       SET bind.deleted = b'1',
           bind.update_time = NOW()
     WHERE bind.deleted = b'0';

    UPDATE `system_users`
      JOIN tmp_v115_seed_users s
        ON s.id = `system_users`.id
       AND s.username = `system_users`.username
       AND s.password_hash = `system_users`.password
       AND s.tenant_id = `system_users`.tenant_id
       SET `status` = 1,
           `deleted` = b'1',
           `update_time` = NOW()
     WHERE `system_users`.deleted = b'0';
END$$
DELIMITER ;

CALL p_v115_production_seed_cleanup();

DROP PROCEDURE IF EXISTS p_v115_production_seed_cleanup;
DROP TEMPORARY TABLE IF EXISTS tmp_v115_seed_users;

-- 禁用 V1.1 中的 OAuth2 演示客户端。此处只使用非敏感标识符，
-- 不写入 client secret；真实 Secret 必须由密钥管理系统轮换并失效旧值。
UPDATE `system_oauth2_client`
   SET `status` = 1,
       `update_time` = NOW()
 WHERE (`id` = 1 AND `client_id` = 'default')
    OR (`id` = 40 AND `client_id` = 'test')
    OR (`id` = 41 AND `client_id` = 'zhicloud-sso-demo-by-code')
    OR (`id` = 42 AND `client_id` = 'zhicloud-sso-demo-by-password');

-- 禁用 V1.1 中的社交演示客户端。此处同样不写入 client_secret。
UPDATE `system_social_client`
   SET `status` = 1,
       `update_time` = NOW()
 WHERE (`id` = 1 AND `client_id` = 'dingvrnreaje3yqvzhxg')
    OR (`id` = 2 AND `client_id` = 'dingtsu9hpepjkbmthhw')
    OR (`id` = 3 AND `client_id` = 'wx5b23ba7a5589ecbb')
    OR (`id` = 43 AND `client_id` = 'wx63c280fe3248a3e7')
    OR (`id` = 44 AND `client_id` = '2')
    OR (`id` = 45 AND `client_id` = '2')
    OR (`id` = 46 AND `client_id` = '2')
    OR (`id` = 47 AND `client_id` = '1');

-- 回滚：本迁移只改变状态与软删除标记，不提供自动回滚；需要恢复时必须先
-- 完成备份校验、密钥轮换和审计取证，并由人工审核后编写新的前向迁移。
