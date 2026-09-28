-- ============================================================
-- V115 人工核验脚本（只读）
--
-- 该脚本只输出 PASS/FAIL，不改变任何业务数据。执行前请先完成备份。
-- ============================================================

WITH expected_seed AS (
    SELECT 1 AS id, 'admin' AS username, '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG' AS password_hash, 1 AS tenant_id
    UNION ALL SELECT 100, 'zhicloud', '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG', 1
    UNION ALL SELECT 103, 'yuanma', '$2a$04$k/d6mc0nySN0i2udwcI8Ee8V5aM5OHixBRbQfXmPuFTUl3Zf/DBs.', 1
    UNION ALL SELECT 104, 'test', '$2a$04$BrwaYn303hjA/6TnXqdGoOLhyHOAA0bVrAFu6.1dJKycqKUnIoRz2', 1
    UNION ALL SELECT 107, 'admin107', '$2a$10$dYOOBKMO93v/.ReCqzyFg.o67Tqk.bbc2bhrpyBGkIw9aypCtr2pm', 118
    UNION ALL SELECT 108, 'admin108', '$2a$10$y6mfvKoNYL1GXWak8nYwVOH.kCWqjactkzdoIDgiKl93WN3Ejg.Lu', 119
    UNION ALL SELECT 109, 'admin109', '$2a$10$JAqvH0tEc0I7dfDVBI7zyuB4E3j.uH6daIjV53.vUS6PknFkDJkuK', 120
    UNION ALL SELECT 110, 'admin110', '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm', 121
    UNION ALL SELECT 111, 'test', '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm', 121
    UNION ALL SELECT 112, 'newobject', '$2a$04$dB0z8Q819fJWz0hbaLe6B.VfHCjYgWx6LFfET5lyz3JwcqlyCkQ4C', 1
    UNION ALL SELECT 113, 'aoteman', '$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO', 122
    UNION ALL SELECT 114, 'hrmgr', '$2a$10$TR4eybBioGRhBmDBWkqWLO6NIh3mzYa8KBKDDB5woiGYFVlRAi.fu', 1
    UNION ALL SELECT 115, 'aotemane', '$2a$04$GcyP0Vyzb2F2Yni5PuIK9ueGxM0tkZGMtDwVRwrNbtMvorzbpNsV2', 1
    UNION ALL SELECT 117, 'admin123', '$2a$04$sEtimsHu9YCkYY4/oqElHem2Ijc9ld20eYO6lN.g/21NfLUTDLB9W', 1
    UNION ALL SELECT 118, 'goudan', '$2a$04$3suGZjnA6rM5bErf38u1felbgqbsPHGdRG3l9NkxPCEt2ah9Y6aJi', 1
    UNION ALL SELECT 139, 'wwbwwb', '$2a$04$FJLIyg8lbPytP29pbZaiU.LesJvCsYfEaHqQfB0pGQhK3e9BeZmLy', 1
    UNION ALL SELECT 141, 'admin1', '$2a$04$oj6F6d7HrZ70kYVD3TNzEu.m3TPUzajOVuC66zdKna8KRerK1FmVa', 1
    UNION ALL SELECT 142, 'test01', '$2a$04$4bCYWZkjxxOC4QE0LY2M9uEEKWeJbLfs489NFtQoyidL5I0FndRaO', 1
    UNION ALL SELECT 143, 'a00001', '$2a$04$GhVHFviOw/SsTmiQtifHJesDYFlHMeGK7OWh7aGCCjGGVCmbHVAwa', 1
    UNION ALL SELECT 144, 'aoteman001', '$2a$04$omQOmhz8OyUFBKw77nr8KOtMp6xdvoQ1gWStjk9r8.OYT3Bv6oEYe', 1
)
SELECT 'V115_SEED_USERS_DISABLED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_users u
             JOIN expected_seed s
               ON s.id = u.id
              AND s.username = u.username
              AND s.password_hash = u.password
              AND s.tenant_id = u.tenant_id
            WHERE u.status = 0
              AND u.deleted = b'0'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_USER_ROLES_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_user_role ur
             JOIN expected_seed s
               ON s.id = ur.user_id
              AND s.tenant_id = ur.tenant_id
            WHERE COALESCE(ur.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_USER_POSTS_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_user_post p
             JOIN expected_seed s
               ON s.id = p.user_id
              AND s.tenant_id = p.tenant_id
            WHERE COALESCE(p.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_OAUTH2_ACCESS_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_oauth2_access_token at
             JOIN expected_seed s
               ON s.id = at.user_id
              AND s.tenant_id = at.tenant_id
            WHERE COALESCE(at.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_OAUTH2_REFRESH_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_oauth2_refresh_token rt
             JOIN expected_seed s
               ON s.id = rt.user_id
              AND s.tenant_id = rt.tenant_id
            WHERE COALESCE(rt.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_OAUTH2_APPROVES_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_oauth2_approve oa
             JOIN expected_seed s
               ON s.id = oa.user_id
              AND s.tenant_id = oa.tenant_id
            WHERE COALESCE(oa.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_OAUTH2_CODES_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_oauth2_code oc
             JOIN expected_seed s
               ON s.id = oc.user_id
              AND s.tenant_id = oc.tenant_id
            WHERE COALESCE(oc.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_SEED_SOCIAL_BINDS_REVOKED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_social_user_bind sb
             JOIN expected_seed s
               ON s.id = sb.user_id
              AND s.tenant_id = sb.tenant_id
            WHERE COALESCE(sb.deleted, b'0') <> b'1'
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_DEMO_OAUTH2_CLIENTS_DISABLED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_oauth2_client
            WHERE (`id` = 1 AND `client_id` = 'default' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 40 AND `client_id` = 'test' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 41 AND `client_id` = 'zhicloud-sso-demo-by-code' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 42 AND `client_id` = 'zhicloud-sso-demo-by-password' AND COALESCE(`status`, 0) <> 1)
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_DEMO_SOCIAL_CLIENTS_DISABLED' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_social_client
            WHERE (`id` = 1 AND `client_id` = 'dingvrnreaje3yqvzhxg' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 2 AND `client_id` = 'dingtsu9hpepjkbmthhw' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 3 AND `client_id` = 'wx5b23ba7a5589ecbb' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 43 AND `client_id` = 'wx63c280fe3248a3e7' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 44 AND `client_id` = '2' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 45 AND `client_id` = '2' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 46 AND `client_id` = '2' AND COALESCE(`status`, 0) <> 1)
               OR (`id` = 47 AND `client_id` = '1' AND COALESCE(`status`, 0) <> 1)
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
UNION ALL
SELECT 'V115_NON_SEED_SUPER_ADMIN_PRESENT' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM system_users u
             JOIN expected_seed s
               ON s.id = u.id
              AND s.username = u.username
              AND s.password_hash = u.password
              AND s.tenant_id = u.tenant_id
            WHERE u.status = 0
              AND u.deleted = b'0'
       ) = 0 OR EXISTS (
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
             LEFT JOIN expected_seed s
               ON s.id = u.id
              AND s.username = u.username
              AND s.password_hash = u.password
              AND s.tenant_id = u.tenant_id
            WHERE u.status = 0
              AND u.deleted = b'0'
              AND r.code = 'super_admin'
              AND r.status = 0
              AND s.id IS NULL
       ) THEN 'PASS' ELSE 'FAIL' END AS verdict;
