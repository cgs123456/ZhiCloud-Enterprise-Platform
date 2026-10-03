-- ============================================================
-- V108: 关闭租户隔离 P0
--
-- 背景：
--   1. crm_clue_pool_config 原 DDL 没有 tenant_id，但 CrmCluePoolConfigDO
--      已改为继承 TenantBaseDO。若不在数据库补列，MyBatis-Plus 租户拦截器会
--      给查询拼接 tenant_id 条件，导致 Unknown column。
--   2. system_sms_code 已有 tenant_id，但索引只覆盖 mobile。验证码校验按
--      (tenant_id, mobile, id) 查询，登录高峰期会退化为按手机号扫描后再过滤租户。
--
-- 处理：
--   1. 为 crm_clue_pool_config 增加 tenant_id，历史行回填默认租户 0。
--   2. 为 crm_clue_pool_config 增加以 tenant_id 为首列的索引。
--   3. 为 system_sms_code 增加 (tenant_id, mobile, id) 索引。
--   4. 所有 DDL 先查 information_schema，重复执行可安全跳过。
--
-- 数据语义：
--   历史 crm_clue_pool_config 行没有可推断的租户归属，统一归属默认租户 0。
--   不会把默认租户配置复制到其他租户；其他租户需要自行保存配置后才能使用
--   线索公海功能。这是防止跨租户配置泄漏的有意选择，不是数据丢失。
--
-- 风险与边界：
--   * 本迁移只证明结构可升级，不证明生产表已完成备份或可在任意时刻执行。
--   * ADD COLUMN / ADD INDEX 在大表上可能触发表重建或长时间 MDL；生产执行前
--     必须评估备份、磁盘空间、复制延迟和业务低峰窗口。
--   * 不添加 (tenant_id) 唯一约束：该表使用 deleted 软删除，直接唯一会阻止
--     删除后重新创建配置；唯一性应由业务层或带 deleted 的复合约束单独评审。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v108_ensure_tenant_isolation;
DELIMITER $$
CREATE PROCEDURE p_v108_ensure_tenant_isolation()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_clue_pool_config'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V108 table missing: crm_clue_pool_config';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_clue_pool_config'
           AND column_name = 'tenant_id'
    ) THEN
        ALTER TABLE `crm_clue_pool_config`
            ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号';
    ELSE
        UPDATE `crm_clue_pool_config`
           SET `tenant_id` = 0
         WHERE `tenant_id` IS NULL;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'crm_clue_pool_config'
               AND column_name = 'tenant_id'
               AND is_nullable = 'YES'
        ) THEN
            ALTER TABLE `crm_clue_pool_config`
                MODIFY COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号';
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_clue_pool_config'
           AND index_name = 'idx_crm_clue_pool_config_tenant'
    ) THEN
        ALTER TABLE `crm_clue_pool_config`
            ADD INDEX `idx_crm_clue_pool_config_tenant` (`tenant_id`, `deleted`);
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_sms_code'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V108 table missing: system_sms_code';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'system_sms_code'
           AND index_name = 'idx_sms_code_tenant_mobile_id'
    ) THEN
        ALTER TABLE `system_sms_code`
            ADD INDEX `idx_sms_code_tenant_mobile_id` (`tenant_id`, `mobile`, `id`);
    END IF;
END$$
DELIMITER ;

CALL p_v108_ensure_tenant_isolation();
DROP PROCEDURE IF EXISTS p_v108_ensure_tenant_isolation;

-- 回滚：删除新增索引不会改变业务语义；删除 crm_clue_pool_config.tenant_id 会
-- 使 TenantBaseDO 查询失效，必须先回退应用代码并完成数据取证，不能单独执行。
