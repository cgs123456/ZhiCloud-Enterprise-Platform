-- ============================================================
-- 生产首次初始化模板：sql/sqlserver/ruoyi-vue-pro_bootstrap.sql
-- 本文件处于“未渲染”状态，两个 __ZHICLOUD_BOOTSTRAP_*__ 占位符必须被替换后才能执行。
-- 渲染方式：
--   python scripts/production/init_bootstrap_admin.py --dialect sqlserver --out bootstrap_rendered.sql
-- 该脚本会生成随机口令（bcrypt cost=12）并只打印一次，请立即存入密钥管理系统。
-- 前置条件：基础 Schema 已初始化，system_role(id=1) 与 system_post(id=4) 存在。
-- 安全约束：不得写入真实邮箱、手机号、已知口令哈希或固定管理员 ID。
-- 执行边界：本模板设计为受控的首次初始化脚本，只允许在一次变更窗口内执行；
--   渲染脚本会再次检查账号是否存在。非 MySQL 模板当前未在目标实库验证。
-- 幂等策略：按 tenant_id=1 + username 预检；发现重复账号立即失败；已存在则更新并重建角色/岗位关系。
-- ============================================================


-- SQL Server：省略 IDENTITY id，使用 SCOPE_IDENTITY() 获取本作用域刚插入的用户 ID。
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @zhicloud_bootstrap_user_count int;
DECLARE @zhicloud_bootstrap_user_id bigint;

SELECT @zhicloud_bootstrap_user_count = COUNT(*)
FROM system_users
WHERE username = N'__ZHICLOUD_BOOTSTRAP_USERNAME__'
  AND tenant_id = 1;

IF @zhicloud_bootstrap_user_count > 1
BEGIN
    THROW 51000, 'duplicate bootstrap username in tenant 1', 1;
END;

SELECT TOP (1) @zhicloud_bootstrap_user_id = id
FROM system_users
WHERE username = N'__ZHICLOUD_BOOTSTRAP_USERNAME__'
  AND tenant_id = 1
ORDER BY id;

IF @zhicloud_bootstrap_user_id IS NULL
BEGIN
    INSERT INTO system_users
    (username, password, nickname, remark, dept_id, post_ids, email, mobile,
     sex, avatar, status, login_ip, login_date, creator, create_time, updater,
     update_time, deleted, tenant_id)
    VALUES (N'__ZHICLOUD_BOOTSTRAP_USERNAME__', N'__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__',
            N'智云', N'生产初始化管理员', 103, N'[4]', N'', N'', 1, NULL, 0, NULL, NULL,
            N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(),
            N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(), 0, 1);
    SET @zhicloud_bootstrap_user_id = SCOPE_IDENTITY();
END
ELSE
BEGIN
    UPDATE system_users
    SET password = N'__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__',
        nickname = N'智云',
        remark = N'生产初始化管理员',
        dept_id = 103,
        post_ids = N'[4]',
        email = N'',
        mobile = N'',
        sex = 1,
        status = 0,
        deleted = 0,
        updater = N'__ZHICLOUD_BOOTSTRAP_USERNAME__',
        update_time = SYSDATETIME(),
        tenant_id = 1
    WHERE id = @zhicloud_bootstrap_user_id
      AND tenant_id = 1;
END;

DELETE FROM system_user_role
 WHERE user_id = @zhicloud_bootstrap_user_id
   AND tenant_id = 1;
INSERT INTO system_user_role
(user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (@zhicloud_bootstrap_user_id, 1, N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(),
        N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(), 0, 1);

DELETE FROM system_user_post
 WHERE user_id = @zhicloud_bootstrap_user_id
   AND tenant_id = 1;
INSERT INTO system_user_post
(user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (@zhicloud_bootstrap_user_id, 4, N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(),
        N'__ZHICLOUD_BOOTSTRAP_USERNAME__', SYSDATETIME(), 0, 1);

COMMIT;
