package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * §12.248（A 组第 5 项）：ERP 付款单/收款单 H2 真表对齐测试。
 *
 * <p>背景：ERP 模块的 H2 schema 此前只有 {@code erp_stock} / {@code inventory_item}
 * 两张表（文件头自述「最小种子」），且**没有任何测试继承 {@code BaseDbUnitTest}**——
 * 即这份 schema 当时没有消费者。付款/收款链的所有用例都是 mock mapper，
 * 于是「列齐不齐、唯一键真的建了没」这类问题只能等到真机才暴露。</p>
 *
 * <p>本类补上真表断言，钉住三件事：
 * ① MyBatis-Plus 按 DO 生成的全列 SELECT/INSERT 在 H2 上可执行（DDL 与 DO 列对齐）；
 * ② V200 建立的两个唯一键（{@code uk_erp_finance_payment_idempotency} 等）**真的生效**——
 * mock 永远证明不了唯一键是否存在；
 * ③ mapper 的 CAS 更新与明细求和走的是真 SQL。</p>
 *
 * <p>不覆盖什么：H2 不复现 InnoDB 的 {@code FOR UPDATE} 锁语义，
 * 锁行为仍以真机并发探针为准（§12.233.12 / §12.247）。</p>
 *
 * @author 智云
 */
public class ErpFinancePaymentReceiptH2TableTest extends BaseDbUnitTest {

    @Resource
    private ErpFinancePaymentMapper paymentMapper;
    @Resource
    private ErpFinancePaymentItemMapper paymentItemMapper;
    @Resource
    private ErpFinanceReceiptMapper receiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper receiptItemMapper;

    private ErpFinancePaymentDO payment(String no, String idempotencyKey, String payloadHash) {
        ErpFinancePaymentDO payment = new ErpFinancePaymentDO();
        payment.setNo(no);
        payment.setSupplierId(1L);
        payment.setAccountId(2L);
        payment.setStatus(10);
        payment.setPaymentTime(LocalDateTime.of(2026, 10, 17, 10, 0));
        payment.setTotalPrice(new BigDecimal("100.0000"));
        payment.setPaymentPrice(new BigDecimal("100.0000"));
        payment.setIdempotencyKey(idempotencyKey);
        payment.setPayloadHash(payloadHash);
        return payment;
    }

    private ErpFinanceReceiptDO receipt(String no, String idempotencyKey, String payloadHash) {
        ErpFinanceReceiptDO receipt = new ErpFinanceReceiptDO();
        receipt.setNo(no);
        receipt.setCustomerId(3L);
        receipt.setAccountId(2L);
        receipt.setStatus(10);
        receipt.setReceiptTime(LocalDateTime.of(2026, 10, 17, 10, 0));
        receipt.setTotalPrice(new BigDecimal("200.0000"));
        receipt.setReceiptPrice(new BigDecimal("200.0000"));
        receipt.setIdempotencyKey(idempotencyKey);
        receipt.setPayloadHash(payloadHash);
        return receipt;
    }

    private ErpFinancePaymentItemDO paymentItem(Long paymentId, Long bizId, BigDecimal price) {
        // 必须每次新建对象：MyBatis-Plus 会把生成的主键回填到入参对象，
        // 重复插入同一对象等于重复主键（那是测试自身的问题，不是被测行为）
        ErpFinancePaymentItemDO item = new ErpFinancePaymentItemDO();
        item.setPaymentId(paymentId);
        item.setBizId(bizId);
        item.setBizType(10);
        item.setPaymentPrice(price);
        return item;
    }

    @Test
    public void test_payment_roundTripThroughIdempotencyAndNoQueries() {
        ErpFinancePaymentDO inserted = payment("HP-H2-0001", "h2-payment-key-1", "hash-1");
        paymentMapper.insert(inserted);

        ErpFinancePaymentDO byKey = paymentMapper.selectByIdempotencyKey("h2-payment-key-1");
        assertNotNull(byKey, "幂等键普通读必须命中");
        assertEquals("HP-H2-0001", byKey.getNo());
        assertEquals("hash-1", byKey.getPayloadHash());
        assertEquals(0, new BigDecimal("100.0000").compareTo(byKey.getPaymentPrice()));

        ErpFinancePaymentDO byKeyForUpdate = paymentMapper.selectByIdempotencyKeyForUpdate("h2-payment-key-1");
        assertNotNull(byKeyForUpdate, "幂等键锁读必须命中（语句含 FOR UPDATE）");
        assertEquals(byKey.getId(), byKeyForUpdate.getId());

        ErpFinancePaymentDO byNo = paymentMapper.selectByNo("HP-H2-0001");
        assertNotNull(byNo, "单号读必须命中");
        assertEquals(byKey.getId(), byNo.getId());
    }

    @Test
    public void test_payment_idempotencyUniqueKeyIsEnforced() {
        paymentMapper.insert(payment("HP-H2-0002", "h2-payment-key-2", "hash-2"));
        // 同租户同幂等键二次插入必须被唯一键挡下——这正是 V200 的核心约束
        assertThrows(DuplicateKeyException.class,
                () -> paymentMapper.insert(payment("HP-H2-0003", "h2-payment-key-2", "hash-3")));
    }

    @Test
    public void test_payment_updateByIdAndStatus_writesStatus() {
        ErpFinancePaymentDO inserted = payment("HP-H2-0004", "h2-payment-key-4", "hash-4");
        paymentMapper.insert(inserted);

        ErpFinancePaymentDO update = new ErpFinancePaymentDO();
        update.setStatus(20);
        assertEquals(1, paymentMapper.updateByIdAndStatus(inserted.getId(), 10, update));

        ErpFinancePaymentDO after = paymentMapper.selectById(inserted.getId());
        assertEquals(20, after.getStatus());
        // 前置态不符时 CAS 必须不生效
        assertEquals(0, paymentMapper.updateByIdAndStatus(inserted.getId(), 10, update));
    }

    @Test
    public void test_receipt_roundTripAndUniqueKey() {
        ErpFinanceReceiptDO inserted = receipt("HS-H2-0001", "h2-receipt-key-1", "hash-r1");
        receiptMapper.insert(inserted);

        ErpFinanceReceiptDO byKey = receiptMapper.selectByIdempotencyKey("h2-receipt-key-1");
        assertNotNull(byKey);
        assertEquals("HS-H2-0001", byKey.getNo());
        assertNotNull(receiptMapper.selectByIdempotencyKeyForUpdate("h2-receipt-key-1"));
        assertNotNull(receiptMapper.selectByNo("HS-H2-0001"));

        assertThrows(DuplicateKeyException.class,
                () -> receiptMapper.insert(receipt("HS-H2-0002", "h2-receipt-key-1", "hash-r2")));
    }

    @Test
    public void test_items_insertAndSumByBiz() {
        ErpFinancePaymentDO inserted = payment("HP-H2-0005", "h2-payment-key-5", "hash-5");
        paymentMapper.insert(inserted);

        paymentItemMapper.insert(paymentItem(inserted.getId(), 77L, new BigDecimal("30.0000")));
        paymentItemMapper.insert(paymentItem(inserted.getId(), 77L, new BigDecimal("30.0000")));

        // 同 biz 的两行金额必须求和为 60（明细表映射 biz_id/biz_type 的真 SQL）
        assertEquals(0, new BigDecimal("60.0000")
                .compareTo(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(77L, 10)));

        ErpFinanceReceiptDO r = receipt("HS-H2-0003", "h2-receipt-key-3", "hash-r3");
        receiptMapper.insert(r);
        ErpFinanceReceiptItemDO rItem = new ErpFinanceReceiptItemDO();
        rItem.setReceiptId(r.getId());
        rItem.setBizId(88L);
        rItem.setBizType(10);
        receiptItemMapper.insert(rItem);
        assertNotNull(receiptItemMapper.selectById(rItem.getId()));
    }
}
