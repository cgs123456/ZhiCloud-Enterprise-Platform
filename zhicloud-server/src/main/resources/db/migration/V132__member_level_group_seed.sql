-- ============================================================
-- V132：会员等级与会员分组基础数据（租户 1）
-- ============================================================
-- 真机复跑会员端时发现两处空表：
--   1) `member_level` 0 行 —— `/app-api/member/user/get` 里 `level` 恒为 null，
--      且已有会员的 `level_id = 0` 指向不存在的等级，「会员等级/折扣」这条业务功能无数据可用；
--   2) `member_group` 0 行 —— 后台按分组筛选会员、会员画像都拿不到候选值。
-- 两者都是**配置型参照数据**（平台自带的等级阶梯与默认分组），不是业务流水，所以由迁移脚本补齐；
-- 真实会员的手机号/地址等组织与个人数据仍由各租户自己录入，本脚本不伪造。
-- 幂等：按 (tenant_id, level) / (tenant_id, name) 预检，重复执行不会插第二份；只把 level_id 仍为 0/NULL 的会员对齐到
-- 其经验值能达到的最高等级，已经人工指定过等级的会员不动。
-- ============================================================

INSERT INTO `member_level` (`id`, `name`, `level`, `experience`, `discount_percent`, `icon`,
                            `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.`id`, t.`name`, t.`level`, t.`experience`, t.`discount_percent`, t.`icon`,
       t.`status`, t.`creator`, t.`create_time`, t.`updater`, t.`update_time`, t.`deleted`, t.`tenant_id`
  FROM (
        SELECT 1 AS `id`, '注册会员' AS `name`, 1 AS `level`,    0 AS `experience`, 100 AS `discount_percent`, '' AS `icon`, 0 AS `status`, '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, 1 AS `tenant_id`
  UNION ALL SELECT 2,   '白银会员',       2,                 100,                  98,                      '',           0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
  UNION ALL SELECT 3,   '黄金会员',       3,                1000,                  95,                      '',           0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
  UNION ALL SELECT 4,   '铂金会员',       4,                5000,                  90,                      '',           0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
  UNION ALL SELECT 5,   '钻石会员',       5,               20000,                  85,                      '',           0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
  ) t
 WHERE NOT EXISTS (
        SELECT 1 FROM `member_level` e
         WHERE e.`tenant_id` = t.`tenant_id` AND e.`level` = t.`level` AND e.`deleted` = b'0'
 );

INSERT INTO `member_group` (`name`, `remark`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
        SELECT '普通会员' AS `name`, '默认分组，注册后未做任何运营标记' AS `remark`, 0 AS `status`, '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, 1 AS `tenant_id`
  UNION ALL SELECT '活跃会员', '近期有下单/签到等行为，用于运营触达',                    0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
  UNION ALL SELECT '高价值会员', '累计消费或经验值较高，用于重点维护',                  0,            '1',                  NOW(),                      '1',                  NOW(),                  b'0',              1
) t
 WHERE NOT EXISTS (
        SELECT 1 FROM `member_group` e
         WHERE e.`tenant_id` = t.`tenant_id` AND e.`name` = t.`name` AND e.`deleted` = b'0'
 );

-- 把没被人工指定过等级的会员，对齐到其经验值可达的最高等级（等级 id 与 level 同号）
UPDATE `member_user` u
   JOIN `member_level` l
     ON l.`id` = (SELECT MAX(l2.`id`)
                    FROM `member_level` l2
                   WHERE l2.`tenant_id` = u.`tenant_id`
                     AND l2.`deleted` = b'0'
                     AND l2.`status` = 0
                     AND l2.`experience` <= u.`experience`)
   SET u.`level_id` = l.`id`
 WHERE u.`deleted` = b'0'
   AND (u.`level_id` IS NULL OR u.`level_id` = 0);
