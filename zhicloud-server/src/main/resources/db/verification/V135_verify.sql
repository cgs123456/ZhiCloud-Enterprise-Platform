-- V135 验证：商城支付应用与模拟渠道种子可用，且幂等预检真的挡住了重复
-- 与迁移内自检分工不同：内自检查"种下去了吗"，本文件查"会不会种重、覆盖是否齐、
-- 通知地址是否还能被当成 URL 用"。

-- 1. 同一租户不得有第二个未删除的 mall 应用（幂等预检的正面证据）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_NO_DUPLICATE_APP PASS'
            ELSE CONCAT('V135_NO_DUPLICATE_APP FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT tenant_id FROM pay_app
         WHERE app_key = 'mall' AND deleted = b'0'
         GROUP BY tenant_id HAVING COUNT(*) > 1) d;

-- 2. 同一应用不得有第二个未删除的 mock 渠道
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_NO_DUPLICATE_CHANNEL PASS'
            ELSE CONCAT('V135_NO_DUPLICATE_CHANNEL FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT app_id FROM pay_channel
         WHERE code = 'mock' AND deleted = b'0'
         GROUP BY app_id HAVING COUNT(*) > 1) d;

-- 3. 覆盖：每个未删除租户都要同时有 mall 应用与挂在它上面的 mock 渠道
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_tenant` t
                   WHERE t.`deleted` = b'0'
                     AND EXISTS (SELECT 1 FROM pay_app a
                                  WHERE a.tenant_id = t.id AND a.app_key = 'mall' AND a.deleted = b'0')
                     AND EXISTS (SELECT 1 FROM pay_channel c
                                  JOIN pay_app a2 ON a2.id = c.app_id
                                 WHERE c.tenant_id = t.id AND c.code = 'mock' AND c.deleted = b'0'
                                   AND a2.app_key = 'mall'))
              = (SELECT COUNT(*) FROM `system_tenant` WHERE `deleted` = b'0')
            THEN 'V135_TENANT_COVERAGE PASS'
            ELSE CONCAT('V135_TENANT_COVERAGE FAIL ',
                        (SELECT COUNT(*) FROM `system_tenant` t
                          WHERE t.`deleted` = b'0'
                            AND EXISTS (SELECT 1 FROM pay_app a
                                         WHERE a.tenant_id = t.id AND a.app_key = 'mall' AND a.deleted = b'0')
                            AND EXISTS (SELECT 1 FROM pay_channel c
                                          JOIN pay_app a2 ON a2.id = c.app_id
                                         WHERE c.tenant_id = t.id AND c.code = 'mock' AND c.deleted = b'0'
                                           AND a2.app_key = 'mall')))
       END AS check_result;

-- 4. 应用必须是启用态（渠道反过来必须停用，那条在迁移自检 3 里）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_APP_ENABLED PASS'
            ELSE CONCAT('V135_APP_ENABLED FAIL ', COUNT(*)) END AS check_result
  FROM pay_app
 WHERE app_key = 'mall' AND deleted = b'0' AND status <> 0;

-- 5. 通知地址得是绝对 URL：PayNotifyServiceImpl 直接对它发 HTTP POST，
--    写成相对路径不会报错，只会"回调永远不来"，所以这条必须机检。
SELECT CASE WHEN COUNT(*) = 0 THEN 'V135_NOTIFY_URL_ABSOLUTE PASS'
            ELSE CONCAT('V135_NOTIFY_URL_ABSOLUTE FAIL ', COUNT(*)) END AS check_result
  FROM pay_app
 WHERE app_key = 'mall' AND deleted = b'0'
   AND (`order_notify_url` NOT LIKE 'http://%' AND `order_notify_url` NOT LIKE 'https://%');
