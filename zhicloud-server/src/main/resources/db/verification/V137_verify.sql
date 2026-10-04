-- V137 验证：钱包充值支付状态列的可用性（到账回调既不 NPE 也能 CAS 命中）
-- 分工：迁移内自检查"存量 NULL 清干净了吗、列还允许空吗"，
-- 本文件查"默认值真的是未支付吗、写入方省略该列时能不能落成行、幂等重跑后形状有没有漂"。

-- 1. 列必须是 NOT NULL，且默认值落在未支付
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_wallet_recharge'
                     AND COLUMN_NAME = 'pay_status'
                     AND IS_NULLABLE = 'NO'
                     AND COLUMN_DEFAULT IN ('b\'0\'', '0')) = 1
            THEN 'V137_DEFAULT_UNPAID PASS'
            ELSE CONCAT('V137_DEFAULT_UNPAID FAIL nullable=',
                        (SELECT IS_NULLABLE FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_wallet_recharge'
                            AND COLUMN_NAME = 'pay_status'),
                        ' default=',
                        (SELECT IFNULL(COLUMN_DEFAULT, 'NONE') FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_wallet_recharge'
                            AND COLUMN_NAME = 'pay_status'))
       END AS check_result;

-- 2. 模拟"建单方省略该列"的写法：插入后必须读得到 false，而不是 null
--    （回调第一行就是 if (recharge.getPayStatus())，null 会直接 unbox NPE）
INSERT INTO `pay_wallet_recharge` (`wallet_id`, `total_price`, `pay_price`, `bonus_price`,
                                   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES (0, 1, 1, 0, 'V137_verify', NOW(), 'V137_verify', NOW(), b'0', 0);
SELECT CASE WHEN (SELECT COUNT(*) FROM `pay_wallet_recharge`
                  WHERE `creator` = 'V137_verify' AND `pay_status` = b'0') = 1
             AND (SELECT COUNT(*) FROM `pay_wallet_recharge`
                  WHERE `creator` = 'V137_verify' AND `pay_status` IS NULL) = 0
            THEN 'V137_OMITTED_COLUMN_READS_FALSE PASS'
            ELSE 'V137_OMITTED_COLUMN_READS_FALSE FAIL 省略该列仍落成 NULL' END AS check_result;
DELETE FROM `pay_wallet_recharge` WHERE `creator` = 'V137_verify';

-- 3. CAS 形状：未支付行能被 `pay_status = 0` 命中（幂等回调依赖它）
SELECT CASE WHEN (SELECT COUNT(*) FROM `pay_wallet_recharge` WHERE `pay_status` = b'0')
            >= (SELECT COUNT(*) FROM `pay_wallet_recharge`) - 1
            THEN 'V137_CAS_MATCHES_UNPAID PASS'
            ELSE 'V137_CAS_MATCHES_UNPAID FAIL 仍有行落在不可比较状态' END AS check_result;

-- 4. 幂等重跑安全：不应因为"列已是 NOT NULL"而留下任何 NULL 行
SELECT CASE WHEN COUNT(*) = 0 THEN 'V137_IDEMPOTENT_NO_NULL PASS'
            ELSE CONCAT('V137_IDEMPOTENT_NO_NULL FAIL ', COUNT(*)) END AS check_result
  FROM `pay_wallet_recharge` WHERE `pay_status` IS NULL;
