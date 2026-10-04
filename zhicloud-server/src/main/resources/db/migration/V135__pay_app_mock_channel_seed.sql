-- ============================================================
-- V135：商城支付应用与模拟渠道种子（按现存租户）
-- ============================================================
-- 为什么缺这两行就跑不通商城：`pay_order` 提交时走
--   PayOrderServiceImpl.validateChannelCanSubmit → validPayApp + validPayChannel，
-- 任一为空就抛 PAY_APP_NOT_FOUND / PAY_CHANNEL_NOT_FOUND，也就是「下单后付不了款」，
-- 而 trade 侧回调 `/app-api/trade/order/update-paid` 永远等不到 —— 订单停在未支付。
-- 全仓复核：V123 只建表（CREATE TABLE pay_app / pay_channel），
-- git grep `INSERT INTO ... pay_app|pay_channel` 在所有 .sql 里命中 0，
-- 之前跑通链路的那两行是有人手工建的（creator 是管理员 userId），干净检出并没有。
--
-- 通知地址是**绝对 URL**：这里按单机开发环境写 http://127.0.0.1:48080，
-- 生产/预发必须按真实对外地址改这张表，否则支付回调会打到自己的回环地址上。
-- 自检只断言**约定后缀**（路径契约），不断言主机，换环境不会假失败也不会假通过。
--
-- 两个地址的**语义不同**，别填错（按代码读出来的，不是猜的）：
--   order_notify_url  = 目标端点本身，pay 直接 POST 它
--                       → AppTradeOrderController(@RequestMapping "/trade/order") + "/update-paid"；
--                         且 trade 侧查询用的 app key 默认就是 'mall'（TradeOrderProperties.payAppKey）。
--   refund_notify_url = **基址**，PayOrderServiceImpl.genChannelOrderNotifyUrl 会再拼 "/" + channelId
--                       → 所以这里存 /admin-api/pay/notify/refund 而**不带** channelId；
--                         把它写成完整带 id 的地址会让回调打到不存在的通道上。
--
-- mock 渠道**故意种成「停用」**：启用它等于任何人无需付款即可把订单标为已支付。
-- 停用态在提交时会被 validPayChannel 判 PAY_CHANNEL_IS_DISABLE（实测存在于该分支），
-- 所以开发/演示要用的需显式启用，生产则应建真实渠道（wx_lite / alipay_pc 等）而不是打开 mock。
--
-- 幂等：pay_app 按 (tenant_id, app_key)，pay_channel 按 (app_id, code)，且只认 deleted = b'0'。
-- ============================================================

INSERT INTO `pay_app` (`app_key`, `name`, `status`, `remark`,
                       `order_notify_url`, `refund_notify_url`, `transfer_notify_url`,
                       `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT 'mall' AS `app_key`, '商城应用' AS `name`, 0 AS `status`,
           '迁移种子：商城交易与支付回调链路所需的应用配置' AS `remark`,
           'http://127.0.0.1:48080/app-api/trade/order/update-paid' AS `order_notify_url`,
           'http://127.0.0.1:48080/admin-api/pay/notify/refund' AS `refund_notify_url`,
           NULL AS `transfer_notify_url`,
           '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`,
           b'0' AS `deleted`, tn.`id` AS `tenant_id`
      FROM `system_tenant` tn
     WHERE tn.`deleted` = b'0'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `pay_app` e
     WHERE e.`tenant_id` = t.`tenant_id` AND e.`app_key` = t.`app_key` AND e.`deleted` = b'0'
);

INSERT INTO `pay_channel` (`code`, `status`, `fee_rate`, `remark`, `app_id`, `config`,
                           `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT 'mock' AS `code`, 1 AS `status`, 0 AS `fee_rate`,
           '迁移种子：模拟渠道，默认停用；仅开发/演示按需启用，生产请配置真实渠道' AS `remark`,
           a.`id` AS `app_id`,
           '{"@class":"cn.zhicloud.module.pay.framework.pay.core.client.impl.NonePayClientConfig"}' AS `config`,
           '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`,
           b'0' AS `deleted`, a.`tenant_id` AS `tenant_id`
      FROM `pay_app` a
     WHERE a.`deleted` = b'0' AND a.`app_key` = 'mall'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `pay_channel` e
     WHERE e.`tenant_id` = t.`tenant_id` AND e.`app_id` = t.`app_id`
       AND e.`code` = t.`code` AND e.`deleted` = b'0'
);

-- 自检 1：每个未删除租户都要有一个启用态的 mall 应用
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_tenant` WHERE `deleted` = b'0')
              = (SELECT COUNT(*) FROM `pay_app` WHERE `app_key` = 'mall' AND `status` = 0 AND `deleted` = b'0')
            THEN 'V135_APP_PER_TENANT PASS'
            ELSE CONCAT('V135_APP_PER_TENANT FAIL ',
                        (SELECT COUNT(*) FROM `pay_app` WHERE `app_key` = 'mall' AND `status` = 0 AND `deleted` = b'0'))
       END AS check_result;

-- 自检 2：回调地址的**路径契约**成立（不比主机，换环境仍有效）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_NOTIFY_CONVENTION PASS'
            ELSE CONCAT('V135_NOTIFY_CONVENTION FAIL ', COUNT(*)) END AS check_result
  FROM `pay_app`
 WHERE `app_key` = 'mall' AND `deleted` = b'0'
   AND (`order_notify_url` NOT LIKE '%/app-api/trade/order/update-paid'
        OR `refund_notify_url` NOT LIKE '%/admin-api/pay/notify/refund');

-- 自检 3：mock 渠道存在且**没有任何一个是启用态**（安全默认，防"不付款即已支付"）
SELECT CASE WHEN (SELECT COUNT(*) FROM `pay_channel` WHERE `code` = 'mock' AND `deleted` = b'0') > 0
             AND (SELECT COUNT(*) FROM `pay_channel` WHERE `code` = 'mock' AND `status` = 0 AND `deleted` = b'0') = 0
            THEN 'V135_MOCK_CHANNEL_DISABLED PASS'
            ELSE CONCAT('V135_MOCK_CHANNEL_DISABLED FAIL enabled=',
                        (SELECT COUNT(*) FROM `pay_channel` WHERE `code` = 'mock' AND `status` = 0 AND `deleted` = b'0'))
       END AS check_result;

-- 自检 4：渠道确实挂在本租户的 mall 应用上，config 是可反序列化的 NonePayClientConfig 形态
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_CHANNEL_LINKED_PASS'
            ELSE CONCAT('V135_CHANNEL_LINKED_FAIL ', COUNT(*)) END AS check_result
  FROM `pay_channel` c
 WHERE c.`code` = 'mock' AND c.`deleted` = b'0'
   AND (c.`app_id` IS NULL
        OR NOT EXISTS (SELECT 1 FROM `pay_app` a
                        WHERE a.`id` = c.`app_id` AND a.`app_key` = 'mall'
                          AND a.`tenant_id` = c.`tenant_id` AND a.`deleted` = b'0')
        OR c.`config` NOT LIKE '%NonePayClientConfig%');
