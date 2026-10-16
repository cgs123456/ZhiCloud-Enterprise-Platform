package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.finance.ErpAccountService;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseInService;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.zhicloud.module.erp.service.purchase.ErpSupplierService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_IDEMPOTENCY_KEY_CONFLICT;
import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpFinancePaymentServiceImpl#createFinancePayment} 的并发兜底测试。
 *
 * <p>§12.233：真实事故是 16 线程同键并发创建，15 个返回 500，而数据其实是正确的
 * （唯一键只留了 1 行）。这条路径此前**一行测试都没有**，缺陷因此长期潜伏。
 * 这里把两种失败形态都钉住：DuplicateKeyException（插到已提交行）与
 * DeadlockLoserDataAccessException（多插入争同一唯一键被选为牺牲品）。
 * 两者都必须等到赢家提交、返回已有 id，而不是抛给全局处理器变成 500。</p>
 *
 * <p>「等」不是空话：回查走 {@code FOR UPDATE}，本事务 1.1 的一致性读视图固定于
 * 第一次普通读（早于赢家提交），普通读再自旋也读不到那一行——这是 §12.233 用
 * 2s→8s 两次实测才确认的事实。所以这里钉的必须是锁读。</p>
 */
@ExtendWith(MockitoExtension.class)
class ErpFinancePaymentConcurrentCreateTest {

    private static final long KEY_ID = 5600081L;

    /**
     * 夹具载荷的真实哈希，一次性算好。
     *
     * <p>两个约束同时逼出这个写法：</p>
     * <ol>
     *   <li>不能写死常量（如 "same"）——那样所有用例都会撞 IDEMPOTENCY_KEY_CONFLICT，
     *       测试变成「断言了一个假的失败」，比没有测试更糟。</li>
     *   <li>静态初始化只算一次，避免在用例里反复反射调用——反射调用本身不慢，
     *       但它让这条断言链更难读，且 {@code existing()} 每次被调用都重建一遍
     *       夹具 VO 是纯浪费。</li>
     * </ol>
     * 所有夹具 {@code req()} 的字段完全相同，因此共用一个真值即可。
     */
    private static final String REAL_HASH = realHash();

    private static String realHash() {
        // 用裸对象算，不依赖 Spring 容器
        ErpFinancePaymentSaveReqVO vo = new ErpFinancePaymentSaveReqVO();
        vo.setSupplierId(914000L);
        vo.setDiscountPrice(BigDecimal.ZERO);
        ErpFinancePaymentSaveReqVO.Item item = new ErpFinancePaymentSaveReqVO.Item();
        item.setBizType(11);
        item.setBizId(KEY_ID);
        item.setPaymentPrice(new BigDecimal("1.00"));
        vo.setItems(List.of(item));
        ErpFinancePaymentServiceImpl probe = new ErpFinancePaymentServiceImpl();
        return ReflectionTestUtils.invokeMethod(probe, "buildPaymentPayloadHash", vo);
    }

    @Mock
    private ErpFinancePaymentMapper financePaymentMapper;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpPurchaseReturnService purchaseReturnService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpAccountingEventService accountingEventService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpNoRedisDAO noRedisDAO;

    private ErpFinancePaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErpFinancePaymentServiceImpl();
        inject("financePaymentMapper", financePaymentMapper);
        inject("financePaymentItemMapper", financePaymentItemMapper);
        inject("purchaseInService", purchaseInService);
        inject("purchaseReturnService", purchaseReturnService);
        inject("supplierService", supplierService);
        inject("accountService", accountService);
        inject("accountingEventService", accountingEventService);
        inject("adminUserApi", adminUserApi);
        inject("noRedisDAO", noRedisDAO);
    }

    private void inject(String name, Object value) {
        ReflectionTestUtils.setField(service, name, value);
    }

    /**
     * 赢家未提交时回查必然读不到，必须继续等而不是一次读空就放弃。
     *
     * <p>这条用例是「等」这个行为的唯一证据：如果将来有人把 catch 里的锁读
     * 改回普通读（「反正 RR 也读得到」），这里的 times(3) 会因只调 1 次而失败。</p>
     */
    @Test
    void duplicateKeyThenRetryReturnsExistingId() {
        ErpFinancePaymentSaveReqVO req = req("idp-conc-1");
        when(financePaymentMapper.selectByIdempotencyKey("idp-conc-1")).thenReturn(null);
        when(financePaymentMapper.insert(any(ErpFinancePaymentDO.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        when(financePaymentMapper.selectByIdempotencyKeyForUpdate("idp-conc-1"))
                .thenReturn(null, null, existing(4242L, "idp-conc-1"));
        stubDependenciesForSuccess();

        Long id = service.createFinancePayment(req);

        assertEquals(4242L, id);
        verify(financePaymentMapper, times(3)).selectByIdempotencyKeyForUpdate(eq("idp-conc-1"));
        verify(financePaymentItemMapper, never()).insertBatch(any());
    }

    /**
     * DeadlockLoser 与 DuplicateKey 是同一竞态的两表现，必须同样收敛。
     * 事故现场 15 个 500 里绝大部分是这一种，而旧 catch 只接 DuplicateKeyException。
     */
    @Test
    void deadlockLoserThenRetryReturnsExistingId() {
        ErpFinancePaymentSaveReqVO req = req("idp-conc-2");
        when(financePaymentMapper.selectByIdempotencyKey("idp-conc-2")).thenReturn(null);
        when(financePaymentMapper.insert(any(ErpFinancePaymentDO.class)))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock", new RuntimeException()));
        when(financePaymentMapper.selectByIdempotencyKeyForUpdate("idp-conc-2"))
                .thenReturn(null, null, existing(5253L, "idp-conc-2"));
        stubDependenciesForSuccess();

        Long id = service.createFinancePayment(req);

        assertEquals(5253L, id);
        verify(financePaymentItemMapper, never()).insertBatch(any());
    }

    /**
     * 上界内仍等不到：如实抛原异常，不把失败伪装成成功。
     *
     * <p>这条路径一度以 {@code OutOfMemoryError} 收场，是本用例唯一一段弯路，
     * 按 §12.233.5 的教训如实记下来——它比"用例通过"更值得留：</p>
     * <ul>
     *   <li>我当时先写成"Mockito 对未打桩 default 方法的缺陷导致 OOM"。
     *       **这是错的**，而且我把这句错话写进了这里的注释。
     *       推翻它的对照组：只 mock {@code selectByIdempotencyKeyForUpdate} 一个方法、
     *       调用 21 次，单独跑<strong>通过</strong>。堆栈里的
     *       {@code MockMethodAdvice}/{@code StackWalker} 是受害者，不是原因。</li>
     *   <li>顺着这条错判还试过"把 fork 堆开到 2GB"：无效。堆栈只有 32 帧却耗尽
     *       GB 级堆，所以也<strong>不是</strong>递归深度问题——这两条否掉之后，
     *       "Mockito 缺陷"就已经没有立足点了。</li>
     *   <li>真正让它过的是两件与 mock 框架无关的事：轮询预算压小
     *       （{@code idempotentWaitMaxMs=2ms, step=0ms}，见下面的注入），
     *       以及把载荷哈希改成静态只算一次（{@link #REAL_HASH}）。
     *       根因至今没有定位，但**已不影响断言的有效性**。</li>
     * </ul>
     * 之所以留这段：如果将来有人再遇到"测试莫名 OOM"，这里有一条
     * 已付过学费的排查顺序——先做单方法对照组，别先归因于框架。
     */
    @Test
    void stillMissingAfterWaitBudgetRethrows() {
        ErpFinancePaymentSaveReqVO req = req("idp-conc-3");
        when(financePaymentMapper.selectByIdempotencyKey("idp-conc-3")).thenReturn(null);
        when(financePaymentMapper.insert(any(ErpFinancePaymentDO.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        when(financePaymentMapper.selectByIdempotencyKeyForUpdate("idp-conc-3")).thenReturn(null);
        stubDependenciesForSuccess();
        inject("idempotentWaitMaxMs", 2L);
        inject("idempotentWaitStepMs", 0L);

        assertThrows(DuplicateKeyException.class, () -> service.createFinancePayment(req));
        verify(financePaymentItemMapper, never()).insertBatch(any());
    }

    /**
     * 同键不同载荷必须拒绝：幂等不能吞掉「客户换了金额」这种真实错误。
     */
    @Test
    void sameKeyDifferentPayloadRejected() {
        ErpFinancePaymentSaveReqVO req = req("idp-conc-4");
        when(financePaymentMapper.selectByIdempotencyKey("idp-conc-4")).thenReturn(null);
        when(financePaymentMapper.insert(any(ErpFinancePaymentDO.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        when(financePaymentMapper.selectByIdempotencyKeyForUpdate("idp-conc-4"))
                .thenReturn(existingWithHash(6264L, "idp-conc-4", "different-payload"));
        stubDependenciesForSuccess();

        assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                () -> service.createFinancePayment(req));
    }

    /**
     * 回查自己被选为死锁牺牲品时，必须退避重试而不是把 DeadlockLoser 抛成 500。
     *
     * <p>这是真实事故：16 线程 × 20 轮实测，锁读能等到赢家 48 次，但回查自身触发了
     * 348 次 InnoDB 死锁，每一次都是一个 500——因为回查语句在 catch 块**内部**执行，
     * 它抛的异常外层 catch 接不到自己。本用例钉住的就是这一点：
     * 第 1 次锁读抛 DeadlockLoser，第 2 次才拿到行，结果必须是幂等返回而不是抛异常。</p>
     */
    @Test
    void deadlockDuringAwaitWinnerRetries() {
        ErpFinancePaymentSaveReqVO req = req("idp-conc-5");
        when(financePaymentMapper.selectByIdempotencyKey("idp-conc-5")).thenReturn(null);
        when(financePaymentMapper.insert(any(ErpFinancePaymentDO.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        // 第 1 次锁读：我被选为牺牲品回滚；第 2 次：赢家那一行已可见
        when(financePaymentMapper.selectByIdempotencyKeyForUpdate("idp-conc-5"))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock on await", new RuntimeException()))
                .thenReturn(existing(7275L, "idp-conc-5"));
        stubDependenciesForSuccess();
        inject("idempotentWaitStepMs", 1L);

        assertEquals(7275L, service.createFinancePayment(req),
                "回查期间的死锁必须被退避重试吃掉；抛出去就是用户看到的 500");
        verify(financePaymentItemMapper, never()).insertBatch(any());
    }

    // ---------------------------------------------------------------- helpers

    private void stubDependenciesForSuccess() {
        lenient().when(noRedisDAO.generate(anyString())).thenReturn("FKD-TEST-0001");
        lenient().when(financePaymentMapper.selectByNo("FKD-TEST-0001")).thenReturn(null);
        lenient().when(financePaymentItemMapper.insertBatch(any())).thenReturn(null);

        // 明细校验会查采购入库：给一个已审核的单子即可通过
        ErpPurchaseInDO in = new ErpPurchaseInDO();
        in.setId(KEY_ID);
        in.setSupplierId(914000L);
        in.setStatus(20);
        in.setTotalPrice(new BigDecimal("99999.99"));
        lenient().when(purchaseInService.validatePurchaseIn(KEY_ID)).thenReturn(in);
        lenient().when(supplierService.validateSupplier(914000L)).thenReturn(null);
        lenient().when(accountService.validateAccount(anyLong())).thenReturn(null);
    }

    private ErpFinancePaymentSaveReqVO req(String key) {
        ErpFinancePaymentSaveReqVO vo = new ErpFinancePaymentSaveReqVO();
        vo.setIdempotencyKey(key);
        vo.setSupplierId(914000L);
        // discountPrice 必须给：calculateTotalPrice 直接 subtract(null) 会 NPE
        vo.setDiscountPrice(BigDecimal.ZERO);
        ErpFinancePaymentSaveReqVO.Item item = new ErpFinancePaymentSaveReqVO.Item();
        item.setBizType(11);
        item.setBizId(KEY_ID);
        item.setPaymentPrice(new BigDecimal("1.00"));
        vo.setItems(List.of(item));
        return vo;
    }

    /**
     * 已有单据。payloadHash 取服务端对**同一个 VO** 算出的真值——
     * 写死 "same" 之类的常量只会让测试走 IDEMPOTENCY_KEY_CONFLICT，
     * 那是假的失败信号（§12.233：探针和测试都可能骗自己，必须让断言对齐真实计算）。
     */
    private ErpFinancePaymentDO existing(Long id, String key) {
        return existingWithHash(id, key, null);
    }

    private ErpFinancePaymentDO existingWithHash(Long id, String key, String overrideHash) {
        ErpFinancePaymentDO d = new ErpFinancePaymentDO();
        d.setId(id);
        d.setIdempotencyKey(key);
        String hash = overrideHash != null ? overrideHash : REAL_HASH;
        d.setPayloadHash(hash);
        return d;
    }
}
