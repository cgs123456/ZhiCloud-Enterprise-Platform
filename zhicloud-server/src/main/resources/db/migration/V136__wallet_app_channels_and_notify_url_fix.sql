-- ============================================================
-- V136：把资金链的支付应用/渠道拓扑补齐，并纠正 V135 种错的两条回调地址
-- ============================================================
-- 本迁移只动"配置型行"，不动任何业务单据。三件事都有代码依据（行号来自本轮核对）：
--
-- 一、V135 把 mall 应用的 refund_notify_url 种错了，这里改回来。
--     pay 侧 `PayRefundServiceImpl:118 .setNotifyUrl(app.getRefundNotifyUrl())` ——
--     pay_app 的这一列存的是**业务方回调地址**，不是渠道回调地址；
--     渠道回调基址另有来源：`PayRefundServiceImpl:186` 用的是
--     `payProperties.getRefundNotifyUrl() + "/" + channelId`（配置文件 zhicloud.pay.refund-notify-url）。
--     所以 V135 写的 /admin-api/pay/notify/refund（那是 pay 自己给渠道用的地址前缀）
--     会让"售后退款成功后通知业务"这一跳永远打不回 trade，售后单永远停在待退款。
--     正确端点：AfterSaleController(@RequestMapping "/trade/after-sale") + "/update-refunded"（@PermitAll）。
--     同理 transfer：`PayTransferServiceImpl:85 .setNotifyUrl(payApp.getTransferNotifyUrl())`，
--     而 V135 留的是 NULL → 提现钱已到账、通知却 POST 空地址，提现单永停在审核通过态。
--     正确端点：BrokerageWithdrawController(@RequestMapping "/trade/brokerage-withdraw") + "/update-transferred"。
--     只改"当前值恰好等于 V135 种错值 / 仍为 NULL"的行，人工配置过的值不覆盖。
--
-- 二、补 wallet 支付应用（钱包充值链的前置）。
--     `PayProperties:18,67` 默认 walletPayAppKey='wallet'（可被 zhicloud.pay.wallet-pay-app-key 覆盖；
--     若部署改了配置，这里的 app_key 要跟着改），而全仓迁移从未种过这一行 ⇒
--     充值第一帧 `PayOrderServiceImpl:118 → PayAppServiceImpl:144` 就抛 PAY_APP_NOT_FOUND，
--     "会员钱包充值"这条流在初始化后的系统里根本进不去。
--     它的业务回调地址 = PayWalletRechargeController 的 /admin-api/pay/wallet-recharge/update-paid（@PermitAll）。
--     refund/transfer 两列留 NULL：钱包应用当前不承载退款与打款回调，种上猜测的地址反而更难发现配错。
--
-- 三、补渠道：mall 应用下的 wallet 渠道（**启用**），wallet 应用下的 mock 渠道（**停用**）。
--     提现打款走 `BrokerageWithdrawServiceImpl:144-158`：appKey 取 tradeOrderProperties.payAppKey（=mall）、
--     channelCode 取 PayChannelEnum.WALLET（="wallet"），而 `PayOrderServiceImpl.validateChannelCanSubmit`
--     找不到渠道就直接拒 ⇒ 没有这一行，"用余额支付/提现到账"整条路不通。
--     wallet 渠道花的是会员自己已有的余额，不是伪造支付成功，所以默认启用；
--     而 wallet 应用下的 mock 渠道一旦启用等于"充值即白拿钱"，与 V135 对 mock 的处置一致：种成停用，
--     需要联调时由人工启用，生产应配真实渠道（wx_lite / alipay_pc 等）。
--     两个渠道的 config 都是 NonePayClientConfig（MockPayClient / WalletPayClient 的泛型参数都是它）。
--
-- 幂等：应用按 (tenant_id, app_key)，渠道按 (app_id, code)，且只认 deleted = b'0' 的行。
-- ============================================================

-- 1. 纠正 mall 应用的两条业务回调地址
UPDATE `pay_app`
   SET `refund_notify_url` = 'http://127.0.0.1:48080/admin-api/trade/after-sale/update-refunded'
 WHERE `app_key` = 'mall' AND `deleted` = b'0'
   AND `refund_notify_url` = 'http://127.0.0.1:48080/admin-api/pay/notify/refund';

UPDATE `pay_app`
   SET `transfer_notify_url` = 'http://127.0.0.1:48080/admin-api/trade/brokerage-withdraw/update-transferred'
 WHERE `app_key` = 'mall' AND `deleted` = b'0'
   AND `transfer_notify_url` IS NULL;

-- 2. 钱包支付应用（按现存租户）
INSERT INTO `pay_app` (`app_key`, `name`, `status`, `remark`,
                       `order_notify_url`, `refund_notify_url`, `transfer_notify_url`,
                       `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT 'wallet' AS `app_key`, '钱包应用' AS `name`, 0 AS `status`,
           '迁移种子：会员钱包充值链所需的支付应用（app_key 需与 zhicloud.pay.wallet-pay-app-key 一致）' AS `remark`,
           'http://127.0.0.1:48080/admin-api/pay/wallet-recharge/update-paid' AS `order_notify_url`,
           NULL AS `refund_notify_url`, NULL AS `transfer_notify_url`,
           '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`,
           b'0' AS `deleted`, tn.`id` AS `tenant_id`
      FROM `system_tenant` tn
     WHERE tn.`deleted` = b'0'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `pay_app` e
     WHERE e.`tenant_id` = t.`tenant_id` AND e.`app_key` = t.`app_key` AND e.`deleted` = b'0'
);

-- 3. 渠道：mall 下启用 wallet；wallet 下停用 mock
INSERT INTO `pay_channel` (`code`, `status`, `fee_rate`, `remark`, `app_id`, `config`,
                           `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT 'wallet' AS `code`, 0 AS `status`, 0 AS `fee_rate`,
           '迁移种子：余额支付渠道（花会员已有余额，非伪造支付）' AS `remark`,
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

INSERT INTO `pay_channel` (`code`, `status`, `fee_rate`, `remark`, `app_id`, `config`,
                           `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT 'mock' AS `code`, 1 AS `status`, 0 AS `fee_rate`,
           '迁移种子：充值联调用模拟渠道，默认停用；启用即等于凭空增发余额，生产必须配真实渠道' AS `remark`,
           a.`id` AS `app_id`,
           '{"@class":"cn.zhicloud.module.pay.framework.pay.core.client.impl.NonePayClientConfig"}' AS `config`,
           '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`, NOW() AS `update_time`,
           b'0' AS `deleted`, a.`tenant_id` AS `tenant_id`
      FROM `pay_app` a
     WHERE a.`deleted` = b'0' AND a.`app_key` = 'wallet'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `pay_channel` e
     WHERE e.`tenant_id` = t.`tenant_id` AND e.`app_id` = t.`app_id`
       AND e.`code` = t.`code` AND e.`deleted` = b'0'
);

-- 自检 1：mall 应用的两条回调地址必须指向业务端点，且不再是 pay 自己的渠道前缀
SELECT CASE WHEN COUNT(*) = 0 THEN 'V136_MALL_NOTIFY_URLS PASS'
            ELSE CONCAT('V136_MALL_NOTIFY_URLS FAIL ', COUNT(*)) END AS check_result
  FROM `pay_app`
 WHERE `app_key` = 'mall' AND `deleted` = b'0'
   AND (`refund_notify_url` NOT LIKE '%/admin-api/trade/after-sale/update-refunded'
        OR `transfer_notify_url` NOT LIKE '%/admin-api/trade/brokerage-withdraw/update-transferred');

-- 自检 2：wallet 应用覆盖到每个未删除租户，且回调地址是钱包充值端点
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_tenant` WHERE `deleted` = b'0')
              = (SELECT COUNT(*) FROM `pay_app`
                  WHERE `app_key` = 'wallet' AND `deleted` = b'0'
                    AND `order_notify_url` LIKE '%/admin-api/pay/wallet-recharge/update-paid')
            THEN 'V136_WALLET_APP_COVERAGE PASS'
            ELSE CONCAT('V136_WALLET_APP_COVERAGE FAIL ',
                        (SELECT COUNT(*) FROM `pay_app`
                          WHERE `app_key` = 'wallet' AND `deleted` = b'0'))
       END AS check_result;

-- 自检 3：mall 下的 wallet 渠道存在且启用（否则余额支付/提现打款无路可走）
SELECT CASE WHEN (SELECT COUNT(*) FROM `pay_channel` c
                   JOIN `pay_app` a ON a.`id` = c.`app_id`
                  WHERE c.`code` = 'wallet' AND c.`deleted` = b'0' AND c.`status` = 0
                    AND a.`app_key` = 'mall' AND a.`deleted` = b'0')
              = (SELECT COUNT(*) FROM `pay_app` WHERE `app_key` = 'mall' AND `deleted` = b'0')
            THEN 'V136_WALLET_CHANNEL_ENABLED PASS'
            ELSE CONCAT('V136_WALLET_CHANNEL_ENABLED FAIL ',
                        (SELECT COUNT(*) FROM `pay_channel` c
                          JOIN `pay_app` a ON a.`id` = c.`app_id`
                         WHERE c.`code` = 'wallet' AND c.`deleted` = b'0' AND c.`status` = 0
                           AND a.`app_key` = 'mall'))
       END AS check_result;

-- 自检 4：wallet 应用下的 mock 渠道一个都不许是启用态（启用即凭空增发余额）
SELECT CASE WHEN (SELECT COUNT(*) FROM `pay_channel` c
                   JOIN `pay_app` a ON a.`id` = c.`app_id`
                  WHERE a.`app_key` = 'wallet' AND c.`code` = 'mock'
                    AND c.`deleted` = b'0' AND c.`status` = 0) = 0
            THEN 'V136_RECHARGE_MOCK_DISABLED PASS'
            ELSE 'V136_RECHARGE_MOCK_DISABLED FAIL 有启用态的充值模拟渠道'
       END AS check_result;
