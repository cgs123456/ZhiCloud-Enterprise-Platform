-- V139 验证：退款成功回调依赖的 refund_point 列的可用性
-- 分工：迁移内自检查"存量 NULL 清干净了吗、列还允许空吗"，
-- 本文件查"默认值真的是 0 吗、建单方省略该列时能不能落成 0、幂等重跑后形状有没有漂"。

-- 1. 列必须是 NOT NULL，且默认值为 0（回调里 getRefundPoint() + usePoint 直接 unbox）
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_order'
                     AND COLUMN_NAME = 'refund_point'
                     AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT = '0') = 1
            THEN 'V139_DEFAULT_ZERO PASS'
            ELSE CONCAT('V139_DEFAULT_ZERO FAIL nullable=',
                        (SELECT IS_NULLABLE FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_order'
                            AND COLUMN_NAME = 'refund_point'),
                        ' default=',
                        (SELECT IFNULL(COLUMN_DEFAULT, 'NONE') FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_order'
                            AND COLUMN_NAME = 'refund_point'))
       END AS check_result;

-- 2. 模拟"建单方省略 refund_point"的写法（缺陷时的真实写法）：插入后必须读得到 0，而不是 null
--    trade_order 只有 id 是无默认的 NOT NULL 列，最小行即可落库
INSERT INTO `trade_order` (`id`, `user_id`, `order_no`, `creator`, `create_time`, `updater`,
                           `update_time`, `deleted`, `tenant_id`)
VALUES (2147483000, 0, 'V139VERIFY', 'V139_verify', NOW(), 'V139_verify', NOW(), b'0', 0);
SELECT CASE WHEN (SELECT COUNT(*) FROM `trade_order`
                  WHERE `creator` = 'V139_verify' AND `refund_point` = 0) = 1
             AND (SELECT COUNT(*) FROM `trade_order`
                  WHERE `creator` = 'V139_verify' AND `refund_point` IS NULL) = 0
            THEN 'V139_OMITTED_COLUMN_READS_ZERO PASS'
            ELSE 'V139_OMITTED_COLUMN_READS_ZERO FAIL 省略该列仍落成 NULL' END AS check_result;

-- 3. 回调算术形状：省略该列的行能被 `order.getRefundPoint() + orderItem.getUsePoint()` 安全累加
--    （0 + 任意整数合法；NULL 参与即 NPE——上一行已保证不会是 NULL）
SELECT CASE WHEN (SELECT `refund_point` FROM `trade_order` WHERE `creator` = 'V139_verify') + 0 = 0
            THEN 'V139_UNBOX_SAFE PASS'
            ELSE 'V139_UNBOX_SAFE FAIL' END AS check_result;
DELETE FROM `trade_order` WHERE `creator` = 'V139_verify';

-- 4. 幂等重跑安全：不应因为"列已是 NOT NULL"而留下任何 NULL 行
SELECT CASE WHEN COUNT(*) = 0 THEN 'V139_IDEMPOTENT_NO_NULL PASS'
            ELSE CONCAT('V139_IDEMPOTENT_NO_NULL FAIL ', COUNT(*)) END AS check_result
  FROM `trade_order` WHERE `refund_point` IS NULL;
