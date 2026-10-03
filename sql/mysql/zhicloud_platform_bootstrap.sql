-- ============================================================
-- 生产首次初始化模板：sql/mysql/zhicloud_platform_bootstrap.sql
-- 本文件处于“未渲染”状态，两个 __ZHICLOUD_BOOTSTRAP_*__ 占位符必须被替换后才能执行。
-- 渲染方式：
--   python scripts/production/init_bootstrap_admin.py --dialect mysql --out bootstrap_rendered.sql
-- 该脚本会生成随机口令（bcrypt cost=12）并只打印一次，请立即存入密钥管理系统。
-- 前置条件：基础 Schema 已初始化，system_role(id=1) 与 system_post(id=4) 存在。
-- 安全约束：不得写入真实邮箱、手机号、已知口令哈希或固定管理员 ID。
-- 执行边界：本模板设计为受控的首次初始化脚本，只允许在一次变更窗口内执行；
--   渲染脚本会再次检查账号是否存在。非 MySQL 模板当前未在目标实库验证。
-- MySQL 执行边界：DELIMITER 是 mysql 命令行客户端指令，不是服务器 SQL；
--   只允许通过 mysql/mysql.exe 的文件输入执行，禁止 JDBC、MyBatis 或普通批量执行器直接执行。
-- 幂等策略：按 tenant_id=1 + username 预检；发现重复账号立即失败；默认执行入口在账号已存在时 fail-closed，只有显式 --force 才允许更新并重建角色/岗位关系。
-- ============================================================


-- MySQL：先由 DBA 账号执行重复账号守卫，再在事务中创建或修复管理员。
DELIMITER $$
DROP PROCEDURE IF EXISTS `zhicloud_bootstrap_guard`$$
CREATE PROCEDURE `zhicloud_bootstrap_guard`()
BEGIN
    DECLARE v_zhicloud_bootstrap_user_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_zhicloud_bootstrap_user_count
      FROM `system_users`
     WHERE `username` = '__ZHICLOUD_BOOTSTRAP_USERNAME__'
       AND `tenant_id` = 1;

    IF v_zhicloud_bootstrap_user_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'duplicate bootstrap username in tenant 1';
    END IF;
END$$
CALL `zhicloud_bootstrap_guard`()$$
DROP PROCEDURE IF EXISTS `zhicloud_bootstrap_guard`$$
DELIMITER ;

START TRANSACTION;

SET @zhicloud_bootstrap_user_id = (
    SELECT `id`
      FROM `system_users`
     WHERE `username` = '__ZHICLOUD_BOOTSTRAP_USERNAME__'
       AND `tenant_id` = 1
     ORDER BY `id`
     LIMIT 1
);

INSERT INTO `system_users`
(`username`, `password`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`,
 `sex`, `avatar`, `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`,
 `update_time`, `deleted`, `tenant_id`)
SELECT '__ZHICLOUD_BOOTSTRAP_USERNAME__', '__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__', '智云',
       '生产初始化管理员', 103, '[4]', '', '', 1, NULL, 0, NULL, NULL,
       '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
       '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, b'0', 1
FROM DUAL
WHERE @zhicloud_bootstrap_user_id IS NULL;

SET @zhicloud_bootstrap_user_id = COALESCE(
    @zhicloud_bootstrap_user_id,
    (SELECT `id`
       FROM `system_users`
      WHERE `username` = '__ZHICLOUD_BOOTSTRAP_USERNAME__'
        AND `tenant_id` = 1
      ORDER BY `id`
      LIMIT 1)
);

UPDATE `system_users`
SET `password` = '__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__',
    `nickname` = '智云',
    `remark` = '生产初始化管理员',
    `dept_id` = 103,
    `post_ids` = '[4]',
    `email` = '',
    `mobile` = '',
    `sex` = 1,
    `status` = 0,
    `deleted` = b'0',
    `updater` = '__ZHICLOUD_BOOTSTRAP_USERNAME__',
    `update_time` = CURRENT_TIMESTAMP,
    `tenant_id` = 1
WHERE `id` = @zhicloud_bootstrap_user_id
  AND `tenant_id` = 1;

DELETE FROM `system_user_role`
 WHERE `user_id` = @zhicloud_bootstrap_user_id
   AND `tenant_id` = 1;
INSERT INTO `system_user_role`
(`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES (@zhicloud_bootstrap_user_id, 1, '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
        '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, b'0', 1);

DELETE FROM `system_user_post`
 WHERE `user_id` = @zhicloud_bootstrap_user_id
   AND `tenant_id` = 1;
INSERT INTO `system_user_post`
(`user_id`, `post_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES (@zhicloud_bootstrap_user_id, 4, '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
        '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, b'0', 1);

COMMIT;
