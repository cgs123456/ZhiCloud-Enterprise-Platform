-- ============================================================
-- V139：修"售后退款成功回调必 NPE"缺陷（trade_order.refund_point 建单时为 NULL）
-- ============================================================
-- 真机跑出来的缺陷（zhicloud_sandbox_clean，仅退款链）：
-- V136 把回调地址指回业务侧后，第一次真实回调即失败——
--   POST /admin-api/trade/after-sale/update-refunded
--   → AfterSaleServiceImpl.updateAfterSaleRefunded:409
--   → TradeOrderUpdateServiceImpl.updateOrderItemWhenAfterSaleSuccess:825
--       Integer orderRefundPoint = order.getRefundPoint() + orderItem.getUsePoint();
--   getRefundPoint() 为 null 直接 unbox NPE → 业务回包 {"code":500,"msg":"系统异常"}
--   → pay 侧 pay_notify_task 记 REQUEST_FAILURE(21)，售后单永远停在"等待平台退款(40)"。
-- 即：**售后退款从来没有可能被标记为完成**——钱退了（pay_refund 成功、余额到账），
-- 单据状态链断在最后一环。与 V136/V137/V138 同族：断点在"建单侧漏列"，
-- 而不是支付或回调机制本身。
--
-- 行级事实：V123 把该列建成 `refund_point INT`（可空、无默认）；建单侧
-- TradeOrderUpdateServiceImpl.createOrder 只显式写了孪生的 refundPrice(0)（:219），
-- refundPoint 从不写 ⇒ 每一张订单落地即为 NULL。同批可空列中 use_point/give_point/
-- refund_price/pay_status 建单都写了（真机行上非 NULL），只有这一列是漏的。
--
-- 改法与 V137/V138 同形：先 UPDATE 填平存量 NULL，再 ALTER 成 NOT NULL DEFAULT 0；
-- 服务侧建单处补显式 setRefundPoint(0)。顺序不能反——NOT NULL 会在存量行上直接失败。
-- ============================================================

UPDATE `trade_order` SET `refund_point` = 0 WHERE `refund_point` IS NULL;

ALTER TABLE `trade_order`
    MODIFY COLUMN `refund_point` int NOT NULL DEFAULT 0
    COMMENT '退还的使用的积分：建单即 0，退款成功回调按它累加（不得为 NULL）';

-- 自检 1：不再有 NULL 的 refund_point（NULL 会让退款成功回调 unbox 即 NPE）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V139_NO_NULL_REFUND_POINT PASS'
            ELSE CONCAT('V139_NO_NULL_REFUND_POINT FAIL ', COUNT(*)) END AS check_result
  FROM `trade_order`
 WHERE `refund_point` IS NULL;

-- 自检 2：列形状本身就是护栏（可空性 + 默认值），不靠人记住建单要赋值
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trade_order'
                     AND COLUMN_NAME = 'refund_point' AND IS_NULLABLE = 'NO') = 1
            THEN 'V139_COLUMN_NOT_NULL PASS'
            ELSE 'V139_COLUMN_NOT_NULL FAIL 列仍可空，漏赋值仍会静默建单' END AS check_result;
