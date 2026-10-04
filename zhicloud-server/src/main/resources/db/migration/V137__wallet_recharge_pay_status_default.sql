-- ============================================================
-- V137：修钱包充值的到账回调必炸缺陷（pay_status 建单时为 NULL）
-- ============================================================
-- 真机跑出来的缺陷，不是猜的：干净库上走一遍"钱包充值 → 支付成功 → 回调到账"，
-- 回调端点 /admin-api/pay/wallet-recharge/update-paid 返回 code=500，服务端日志：
--   java.lang.NullPointerException: Cannot invoke "java.lang.Boolean.booleanValue()"
--       because the return value of PayWalletRechargeDO.getPayStatus() is null
--   （PayWalletRechargeServiceImpl:128 与 :287 都是直接 unbox）
-- 根因是列的形状：pay_wallet_recharge.pay_status 原本 bit(1) NULL 且**无默认值**，
-- 而建单路径（createWalletRecharge → INSTANCE.convert → insert）从不写这一列，
-- 所以每一笔充值单落地时都是 NULL。后果有两层：
--   1) 回调第一行就 NPE（上面的栈）；
--   2) 即便绕过 NPE，幂等 CAS `updateByIdAndPaid(id, false, ...)` 生成的条件是
--      `pay_status = 0`，而 SQL 里 `NULL = 0` 不为真 ⇒ 更新 0 行 ⇒ 抛
--      WALLET_RECHARGE_UPDATE_PAID_STATUS_NOT_UNPAID。
-- 也就是说：**会员钱包充值从来没有可能到账**，与支付应用/渠道怎么配无关
-- （V136 补的 wallet 应用只是让流程第一次走到这一步）。
--
-- 改法：把"未支付"变成列的默认值，并把存量 NULL 行填平——
-- 先 UPDATE 再 ALTER，否则 NOT NULL 会在存量行上失败。
-- 同一张表的 refund_status 本次**没动**：它是 Integer 且服务里用 equals 比较，
-- 不存在 unbox NPE；把两件事混在一条迁移里反而看不清各自的风险。
-- ============================================================

UPDATE `pay_wallet_recharge` SET `pay_status` = b'0' WHERE `pay_status` IS NULL;

ALTER TABLE `pay_wallet_recharge`
    MODIFY COLUMN `pay_status` bit(1) NOT NULL DEFAULT b'0'
    COMMENT '是否已支付：建单即未支付，支付回调用它做幂等 CAS（不得为 NULL）';

-- 自检 1：库里不再有 NULL 的支付状态
SELECT CASE WHEN COUNT(*) = 0 THEN 'V137_NO_NULL_PAY_STATUS PASS'
            ELSE CONCAT('V137_NO_NULL_PAY_STATUS FAIL ', COUNT(*)) END AS check_result
  FROM `pay_wallet_recharge`
 WHERE `pay_status` IS NULL;

-- 自检 2：幂等重跑后列仍是 NOT NULL + 默认未支付（列形状就是这条流的护栏，不能靠记忆）
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_wallet_recharge'
                     AND COLUMN_NAME = 'pay_status' AND IS_NULLABLE = 'NO') = 1
            THEN 'V137_COLUMN_NOT_NULL PASS'
            ELSE 'V137_COLUMN_NOT_NULL FAIL 列仍可空，回调 CAS 仍会打不中' END AS check_result;
