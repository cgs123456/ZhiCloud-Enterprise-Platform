-- V136 验证：资金链的支付应用/渠道拓扑形状正确，且没有把"渠道回调地址"当"业务回调地址"存
-- 与迁移内自检的分工：内自检查"该指的地址指对了吗、覆盖够不够"，
-- 本文件查"有没有重复行 / 跨租户错挂 / 把整类地址填反了 / config 反序列化得了吗"。

-- 1. 任何应用都不许把 pay 自己的渠道回调前缀当成业务回调地址（V135 当初就犯在这一条上）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V136_NO_SELF_LOOP_URL PASS'
            ELSE CONCAT('V136_NO_SELF_LOOP_URL FAIL ', COUNT(*)) END AS check_result
  FROM `pay_app`
 WHERE `deleted` = b'0'
   AND (`order_notify_url` LIKE '%/admin-api/pay/notify/%'
        OR `refund_notify_url` LIKE '%/admin-api/pay/notify/%');

-- 2. 每个未删除租户都齐三件套：mall + wallet 应用、mall 下有 wallet 渠道、渠道挂在本租户应用上
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_tenant` t
                   WHERE t.`deleted` = b'0'
                     AND EXISTS (SELECT 1 FROM `pay_app` ma
                                  WHERE ma.`tenant_id` = t.`id` AND ma.`app_key` = 'mall' AND ma.`deleted` = b'0')
                     AND EXISTS (SELECT 1 FROM `pay_app` wa
                                  WHERE wa.`tenant_id` = t.`id` AND wa.`app_key` = 'wallet' AND wa.`deleted` = b'0')
                     AND EXISTS (SELECT 1 FROM `pay_channel` c
                                  JOIN `pay_app` a2 ON a2.`id` = c.`app_id`
                                 WHERE c.`tenant_id` = t.`id` AND c.`code` = 'wallet' AND c.`deleted` = b'0'
                                   AND a2.`app_key` = 'mall' AND a2.`tenant_id` = t.`id`))
              = (SELECT COUNT(*) FROM `system_tenant` WHERE `deleted` = b'0')
            THEN 'V136_TENANT_TOPOLOGY PASS'
            ELSE CONCAT('V136_TENANT_TOPOLOGY FAIL ',
                        (SELECT COUNT(*) FROM `system_tenant` t
                          WHERE t.`deleted` = b'0'
                            AND EXISTS (SELECT 1 FROM `pay_app` wa
                                         WHERE wa.`tenant_id` = t.`id` AND wa.`app_key` = 'wallet'
                                           AND wa.`deleted` = b'0')
                            AND EXISTS (SELECT 1 FROM `pay_channel` c
                                          JOIN `pay_app` a2 ON a2.`id` = c.`app_id`
                                         WHERE c.`tenant_id` = t.`id` AND c.`code` = 'wallet'
                                           AND c.`deleted` = b'0' AND a2.`app_key` = 'mall')))
       END AS check_result;

-- 3. 渠道不得跨租户挂到别的应用上（app 的 tenant_id 必须与渠道一致）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V136_CHANNEL_TENANT_MATCH PASS'
            ELSE CONCAT('V136_CHANNEL_TENANT_MATCH FAIL ', COUNT(*)) END AS check_result
  FROM `pay_channel` c
  JOIN `pay_app` a ON a.`id` = c.`app_id`
 WHERE c.`deleted` = b'0' AND a.`deleted` = b'0' AND c.`tenant_id` <> a.`tenant_id`;

-- 4. 幂等：应用与渠道都不许出现重复行
SELECT CASE WHEN (SELECT COUNT(*) FROM (SELECT `tenant_id`, `app_key` FROM `pay_app`
                                        WHERE `deleted` = b'0'
                                          AND `app_key` IN ('mall', 'wallet')
                                        GROUP BY `tenant_id`, `app_key` HAVING COUNT(*) > 1) x) = 0
             AND (SELECT COUNT(*) FROM (SELECT `app_id`, `code` FROM `pay_channel`
                                         WHERE `deleted` = b'0'
                                           AND `code` IN ('wallet', 'mock')
                                         GROUP BY `app_id`, `code` HAVING COUNT(*) > 1) y) = 0
            THEN 'V136_NO_DUPLICATE_ROWS PASS'
            ELSE 'V136_NO_DUPLICATE_ROWS FAIL'
       END AS check_result;

-- 5. 新种的两类渠道 config 必须是 NonePayClientConfig 形态（客户端泛型就是它，写错会在建客户端时才炸）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V136_CHANNEL_CONFIG_PARSEABLE PASS'
            ELSE CONCAT('V136_CHANNEL_CONFIG_PARSEABLE FAIL ', COUNT(*)) END AS check_result
  FROM `pay_channel`
 WHERE `deleted` = b'0' AND `code` IN ('wallet', 'mock')
   AND `config` NOT LIKE '%NonePayClientConfig%';
