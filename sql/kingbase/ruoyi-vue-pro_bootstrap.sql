-- ============================================================
-- 生产首次初始化模板：sql/kingbase/ruoyi-vue-pro_bootstrap.sql
-- 本文件处于“未渲染”状态，两个 __ZHICLOUD_BOOTSTRAP_*__ 占位符必须被替换后才能执行。
-- 渲染方式：
--   python scripts/production/init_bootstrap_admin.py --dialect kingbase --out bootstrap_rendered.sql
-- 该脚本会生成随机口令（bcrypt cost=12）并只打印一次，请立即存入密钥管理系统。
-- 前置条件：基础 Schema 已初始化，system_role(id=1) 与 system_post(id=4) 存在。
-- 安全约束：不得写入真实邮箱、手机号、已知口令哈希或固定管理员 ID。
-- 执行边界：本模板设计为受控的首次初始化脚本，只允许在一次变更窗口内执行；
--   渲染脚本会再次检查账号是否存在。非 MySQL 模板当前未在目标实库验证。
-- 幂等策略：按 tenant_id=1 + username 预检；发现重复账号立即失败；已存在则更新并重建角色/岗位关系。
-- ============================================================


-- Kingbase：应用使用 @KeySequence("system_users_seq")，显式取序列并保留会话内 CURRVAL。
-- 按 tenant_id + username 预检，重复账号阻断，存在则更新并重建关系。
BEGIN;

DO $zhicloud_bootstrap$
DECLARE
    v_zhicloud_bootstrap_user_count bigint;
    v_zhicloud_bootstrap_user_id bigint;
BEGIN
    SELECT COUNT(*)
      INTO v_zhicloud_bootstrap_user_count
      FROM system_users
     WHERE username = '__ZHICLOUD_BOOTSTRAP_USERNAME__'
       AND tenant_id = 1;

    IF v_zhicloud_bootstrap_user_count > 1 THEN
        RAISE EXCEPTION 'duplicate bootstrap username in tenant 1';
    END IF;

    IF v_zhicloud_bootstrap_user_count = 0 THEN
        INSERT INTO system_users
        (id, username, password, nickname, remark, dept_id, post_ids, email, mobile,
         sex, avatar, status, login_ip, login_date, creator, create_time, updater,
         update_time, deleted, tenant_id)
        VALUES (nextval('system_users_seq'), '__ZHICLOUD_BOOTSTRAP_USERNAME__',
                '__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__', '智云', '生产初始化管理员', 103, '[4]', '', '',
                1, NULL, 0, NULL, NULL, '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
                '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, 0, 1)
        RETURNING id INTO v_zhicloud_bootstrap_user_id;
    ELSE
        SELECT id
          INTO v_zhicloud_bootstrap_user_id
          FROM system_users
         WHERE username = '__ZHICLOUD_BOOTSTRAP_USERNAME__'
           AND tenant_id = 1;

        UPDATE system_users
           SET password = '__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__',
               nickname = '智云',
               remark = '生产初始化管理员',
               dept_id = 103,
               post_ids = '[4]',
               email = '',
               mobile = '',
               sex = 1,
               status = 0,
               deleted = 0,
               updater = '__ZHICLOUD_BOOTSTRAP_USERNAME__',
               update_time = CURRENT_TIMESTAMP,
               tenant_id = 1
         WHERE id = v_zhicloud_bootstrap_user_id
           AND tenant_id = 1;
    END IF;

    DELETE FROM system_user_role
     WHERE user_id = v_zhicloud_bootstrap_user_id
       AND tenant_id = 1;
    INSERT INTO system_user_role
    (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES (nextval('system_user_role_seq'), v_zhicloud_bootstrap_user_id, 1,
            '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
            '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, 0, 1);

    DELETE FROM system_user_post
     WHERE user_id = v_zhicloud_bootstrap_user_id
       AND tenant_id = 1;
    INSERT INTO system_user_post
    (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES (nextval('system_user_post_seq'), v_zhicloud_bootstrap_user_id, 4,
            '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP,
            '__ZHICLOUD_BOOTSTRAP_USERNAME__', CURRENT_TIMESTAMP, 0, 1);
END
$zhicloud_bootstrap$;

COMMIT;
