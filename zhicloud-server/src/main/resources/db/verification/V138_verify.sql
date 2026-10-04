-- V138 验证：提现状态列的可用性（审核校验与 CAS 都能命中"审核中"）
-- 分工：迁移内自检查"存量 NULL 清干净了吗、列还允许空吗"，
-- 本文件查"默认值真的是审核中吗、写入方省略该列时能不能落成可审核的行、幂等重跑后形状有没有漂"。

-- 1. 列必须是 NOT NULL，且默认值落在审核中(0)
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_brokerage_withdraw'
                     AND COLUMN_NAME = 'status'
                     AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT = '0') = 1
            THEN 'V138_DEFAULT_AUDITING PASS'
            ELSE CONCAT('V138_DEFAULT_AUDITING FAIL nullable=',
                        (SELECT IS_NULLABLE FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_brokerage_withdraw'
                            AND COLUMN_NAME = 'status'),
                        ' default=',
                        (SELECT IFNULL(COLUMN_DEFAULT, 'NONE') FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_brokerage_withdraw'
                            AND COLUMN_NAME = 'status'))
       END AS check_result;

-- 2. 模拟"建单方省略 status"的写法（现状就是这种写法）：插入后必须读得到 0，而不是 null
--    审核第 1 步是 ObjectUtil.notEqual(AUDITING, status)，null 直接判失败
INSERT INTO `trade_brokerage_withdraw` (`user_id`, `price`, `fee_price`, `type`, `user_name`, `user_account`,
                                        `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES (0, 1, 0, 1, 'V138_verify', 'V138_verify', 'V138_verify', NOW(), 'V138_verify', NOW(), b'0', 0);
SELECT CASE WHEN (SELECT COUNT(*) FROM `trade_brokerage_withdraw`
                  WHERE `creator` = 'V138_verify' AND `status` = 0) = 1
             AND (SELECT COUNT(*) FROM `trade_brokerage_withdraw`
                  WHERE `creator` = 'V138_verify' AND `status` IS NULL) = 0
            THEN 'V138_OMITTED_COLUMN_READS_AUDITING PASS'
            ELSE 'V138_OMITTED_COLUMN_READS_AUDITING FAIL 省略该列仍落成 NULL' END AS check_result;

-- 3. CAS 形状：省略 status 的行能被审核用的 `WHERE id = ? AND status = 0` 打中
--    （updateByIdAndStatus 命中 0 行就抛 BROKERAGE_WITHDRAW_STATUS_NOT_AUDITING）
UPDATE `trade_brokerage_withdraw` SET `status` = 10, `audit_time` = NOW()
 WHERE `creator` = 'V138_verify' AND `status` = 0;
SELECT CASE WHEN (SELECT COUNT(*) FROM `trade_brokerage_withdraw`
                  WHERE `creator` = 'V138_verify' AND `status` = 10) = 1
            THEN 'V138_CAS_HITS_NEW_ROW PASS'
            ELSE 'V138_CAS_HITS_NEW_ROW FAIL 审核 CAS 仍打不中新建行' END AS check_result;
DELETE FROM `trade_brokerage_withdraw` WHERE `creator` = 'V138_verify';

-- 4. 幂等重跑安全：不应因为"列已是 NOT NULL"而留下任何 NULL 行
SELECT CASE WHEN COUNT(*) = 0 THEN 'V138_IDEMPOTENT_NO_NULL PASS'
            ELSE CONCAT('V138_IDEMPOTENT_NO_NULL FAIL ', COUNT(*)) END AS check_result
  FROM `trade_brokerage_withdraw` WHERE `status` IS NULL;
