-- ============================================================
-- V138：修佣金提现"审核永远过不去"缺陷（status 建单时为 NULL）
-- ============================================================
-- 真机跑出来的缺陷：干净库上走"申请提现 → 运营审核通过"，
-- 建单成功（/app-api/trade/brokerage-withdraw/create 返回 id），
-- 但审核端点 /admin-api/trade/brokerage-withdraw/approve 返回
--   code=1011008001（提现申请不为待审核状态）
-- 查库看到根因：trade_brokerage_withdraw 的两行 status 都是 NULL。
-- 原因在服务里——BrokerageWithdrawServiceImpl.createBrokerageWithdraw
-- 只 `BeanUtils.toBean(reqVO, DO).setUserId(..).setFeePrice(..)` 后直接 insert，
-- 而 status 既不在 AppBrokerageWithdrawCreateReqVO 的字段里，也没被显式赋值，
-- 列本身又是 `int NULL 且无默认值` ⇒ 每一笔提现单落地即为 NULL。
-- 而审核路径要求 withdraw.getStatus() 必须等于 AUDITING(0)：
--   ObjectUtil.notEqual(0, null) 为真 ⇒ 抛 BROKERAGE_WITHDRAW_STATUS_NOT_AUDITING。
-- 即使绕过这道校验，第 2 步的 CAS `updateByIdAndStatus(id, status, ...)` 生成
-- `WHERE status = NULL`，SQL 里恒不成立 ⇒ 更新 0 行 ⇒ 同样抛错。
-- 也就是说：**佣金提现从来没有可能被审核通过并打款**，
-- 与 pay_transfer、转账渠道、回调地址怎么配都无关
-- （V136 补的转账回调地址只让"通过后"那一段有了去处）。
--
-- 改法与 V137 同形：先 UPDATE 填平存量 NULL 行，再 ALTER 把"审核中"变成列默认值。
-- 顺序不能反——NOT NULL 会在存量行上直接失败。
-- 与 status 同批落 NULL 的还有 total_price（DO 注释为"当前总佣金"快照）：
-- 它不在任何校验/CAS 条件里，只影响列表展示，本次**不动**，另记台账待裁决。
-- ============================================================

UPDATE `trade_brokerage_withdraw` SET `status` = 0 WHERE `status` IS NULL;

ALTER TABLE `trade_brokerage_withdraw`
    MODIFY COLUMN `status` int NOT NULL DEFAULT 0
    COMMENT '提现状态：建单即审核中(0)，审核与打款都按它做 CAS（不得为 NULL）';

-- 自检 1：不再有 NULL 状态的提现单（NULL 会让审核校验和 CAS 双双打不中）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V138_NO_NULL_STATUS PASS'
            ELSE CONCAT('V138_NO_NULL_STATUS FAIL ', COUNT(*)) END AS check_result
  FROM `trade_brokerage_withdraw`
 WHERE `status` IS NULL;

-- 自检 2：列形状本身就是护栏（可空性 + 默认值），不靠人记住建单要赋值
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_brokerage_withdraw'
                     AND COLUMN_NAME = 'status' AND IS_NULLABLE = 'NO') = 1
            THEN 'V138_COLUMN_NOT_NULL PASS'
            ELSE 'V138_COLUMN_NOT_NULL FAIL 列仍可空，漏赋值仍会静默建单' END AS check_result;
