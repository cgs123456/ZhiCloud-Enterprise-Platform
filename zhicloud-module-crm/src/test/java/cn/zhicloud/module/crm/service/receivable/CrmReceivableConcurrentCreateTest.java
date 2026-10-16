package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmReceivableServiceImpl#createReceivable} 幂等键撞唯一键时的兜底行为测试。
 *
 * <p><b>为什么需要这个测试</b>：这条路径此前一行测试都没有
 * （{@code CrmReceivableServiceImplTest} 只测同键<b>顺序</b>重试，
 * {@code CrmReceivableServiceConcurrencyTest} 只测乐观锁与 BPM claim 顺序）。
 * 而 ERP 侧同源缺陷正是因为「唯一键冲突 + 赢家未提交」这条路径没被覆盖，
 * 才在 16 线程并发下暴露成 15 个 500（见台账 §12.233）。</p>
 *
 * <p><b>测试形态说明（重要，别误读成"并发已验证"）</b>：锁序与死锁的结论
 * 全部来自台账 §12.233.12 的<b>真机实测</b>（16 线程 × 20 轮：320/320 全成功、
 * 0 死锁、每键恰好 1 行）。本类是纯 Mockito 单测，用
 * {@code insert → 抛 DuplicateKeyException} 把代码<b>逼进</b> catch 分支，
 * 钉的是<b>兜底逻辑本身</b>（撞键后回查、幂等返回、绝不新建第二行、
 * 绝不吞掉载荷不一致）。H2 单节点没有 MySQL RR 一致性读视图，
 * 量不出真机的锁行为，所以这里刻意不去伪造它——
 * 真机的证据链见 §12.233.12，不由本类负责。</p>
 *
 * <p><b>为什么不用真 H2 库测</b>（试过，如实记）：在同一个外层事务里先插赢家再调
 * service，输家在第 1 步的 {@code selectByIdempotencyKey} 普通读就已经能看到
 * 未提交的赢家行，于是走的是预热检返回，<b>根本到不了 catch</b>；
 * 改用 REQUIRES_NEW 独立事务，输家又在赢家未提交的行上阻塞等锁
 * （H2 报 CannotAcquireLock），同样到不了 catch。也就是说 H2 形态下
 * 这两个分支<b>不可同时满足</b>——所以改用 mock 直接构造 INSERT 撞键的瞬间。
 * 真机上这两个前提天然成立不同事、不同连接），H2 单连接做不到。</p>
 *
 * @author 智云
 */
public class CrmReceivableConcurrentCreateTest {

    private static final Long CONTRACT_ID = 6500050L;
    private static final Long OWNER_USER_ID = 900100L;
    private static final String IDEMPOTENCY_KEY = "concurrent-dup-key-001";

    private CrmReceivableServiceImpl service;

    private CrmReceivableMapper receivableMapper;
    private CrmContractMapper contractMapper;
    private CrmPermissionService permissionService;

    @BeforeEach
    void setUp() {
        service = new CrmReceivableServiceImpl();
        inject();
    }

    private void inject() {
        receivableMapper = mock(CrmReceivableMapper.class);
        contractMapper = mock(CrmContractMapper.class);
        ReflectionTestUtils.setField(service, "receivableMapper", receivableMapper);
        ReflectionTestUtils.setField(service, "contractMapper", contractMapper);

        CrmContractService contractService = mock(CrmContractService.class);
        ReflectionTestUtils.setField(service, "contractService", contractService);

        CrmReceivablePlanService planService = mock(CrmReceivablePlanService.class);
        ReflectionTestUtils.setField(service, "receivablePlanService", planService);

        CrmPermissionService permissionService = mock(CrmPermissionService.class);
        this.permissionService = permissionService;
        ReflectionTestUtils.setField(service, "permissionService", permissionService);

        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);

        BpmProcessInstanceApi bpmApi = mock(BpmProcessInstanceApi.class);
        ReflectionTestUtils.setField(service, "bpmProcessInstanceApi", bpmApi);

        CrmNoRedisDAO noRedisDAO = mock(CrmNoRedisDAO.class);
        ReflectionTestUtils.setField(service, "noRedisDAO", noRedisDAO);
        lenient().when(noRedisDAO.generate(anyString())).thenReturn("HK20261016000001");
    }

    /**
     * 撞唯一键后必须幂等返回赢家的 id，而不是抛 500、也不是新建第二行。
     *
     * <p>赢家的 {@code payloadHash} 必须按服务端同套公式算出来，不能瞎写——
     * 否则 {@code resolveIdempotentReceivable} 会抛 IDEMPOTENCY_KEY_CONFLICT，
     * 测试就变成「断言了一个假的失败」（ERP 侧 §12.233 踩过同类的坑）。</p>
     */
    @Test
    void duplicateKeyAfterWinnerReturnsExistingId() {
        stubForCreate();
        // 预热检两次都读不到赢家（真机上赢家还在另一个事务里未提交）
        when(receivableMapper.selectByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        // INSERT 撞唯一键
        when(receivableMapper.insert(any(CrmReceivableDO.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'idempotency_key'"));
        // 锁读回查拿到赢家那一行
        when(receivableMapper.selectByIdempotencyKeyForUpdate(IDEMPOTENCY_KEY))
                .thenReturn(winner(payloadHashOf(req())));

        Long id = service.createReceivable(req());

        assertEquals(WINNER_ID, id,
                "撞唯一键后必须幂等返回赢家的 id，而不是新建第二行或抛异常");
        // 兜底路径返回，绝不能把权限/明细做成第二份
        verify(permissionService, org.mockito.Mockito.never()).createPermission(any());
    }

    /**
     * 同键但<b>载荷不同</b>必须拒绝：幂等不能吞掉「换了金额」这种真实错误。
     *
     * <p>这条与上一条是同一枚硬币的两面，必须同时存在：只测「同键同载荷幂等返回」
     * 会掩盖「同键不同载荷也被幂等吞掉」这种更危险的行为。</p>
     */
    @Test
    void duplicateKeyDifferentPayloadRejected() {
        stubForCreate();
        when(receivableMapper.selectByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(receivableMapper.insert(any(CrmReceivableDO.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'idempotency_key'"));
        // 赢家的载荷指纹是旧金额算出来的 → 等价于「客户端改了金额重试」
        when(receivableMapper.selectByIdempotencyKeyForUpdate(IDEMPOTENCY_KEY))
                .thenReturn(winner("stale-hash-from-a-different-payload"));

        assertThrows(ServiceException.class, () -> service.createReceivable(req()),
                "同幂等键但载荷不同必须拒绝，不能幂等返回第一行的 id");
        verify(permissionService, org.mockito.Mockito.never()).createPermission(any());
    }

    /**
     * 撞键后回查<b>读不到</b>行：必须如实抛原异常，不许自己造一行蒙过去。
     *
     * <p>这条守住「幂等不会凭空造成功」——真机上若赢家在此刻回滚，
     * 输家撞的是一个正在消失的唯一键，必须把失败还给调用方。</p>
     */
    @Test
    void duplicateKeyButWinnerGoneRethrows() {
        stubForCreate();
        when(receivableMapper.selectByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(receivableMapper.insert(any(CrmReceivableDO.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'idempotency_key'"));
        when(receivableMapper.selectByIdempotencyKeyForUpdate(IDEMPOTENCY_KEY))
                .thenReturn(null);

        assertThrows(DuplicateKeyException.class, () -> service.createReceivable(req()),
                "撞键后回查不到行时必须抛原异常，不能伪造成功");
    }

    /**
     * 锁读只做<b>一次</b>，拿不到就抛。
     *
     * <p>这条是 §12.233.12 实测结论的<b>反面登记</b>：CRM 靠合同行锁把 16 个线程
     * 串行化（赢家之后每个输家的 INSERT 间隔 74~242ms 且从不重叠），
     * 所以每个输家第一次锁读就拿到行、<b>退避循环一次都没触发</b>。
     * 本测试因此确认「只锁读一次」的现状，并把它与 ERP 需要退避重试的形态对照
     * （ERP 建单前没有任何锁，见 {@code ErpFinancePaymentServiceImpl#awaitWinner}）。</p>
     *
     * <p><b>注意本条的定位</b>：它不是「兜底够用」的证明，而是<b>把前提钉住</b>——
     * 若将来有人在 INSERT 之前加入不串行化的写路径，这条会失败，
     * 提示必须补 ERP 那样的退避重试。</p>
     */
    @Test
    void lockReadIsSingleShot() {
        stubForCreate();
        when(receivableMapper.selectByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(receivableMapper.insert(any(CrmReceivableDO.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        // 第一次锁读就返回 null：没有第二次
        when(receivableMapper.selectByIdempotencyKeyForUpdate(IDEMPOTENCY_KEY))
                .thenReturn(null);

        assertThrows(DuplicateKeyException.class, () -> service.createReceivable(req()));
        verify(receivableMapper, times(1)).selectByIdempotencyKeyForUpdate(eq(IDEMPOTENCY_KEY));
    }

    // ---------------------------------------------------------------- helpers

    private static final Long WINNER_ID = randomLongId();

    private CrmReceivableSaveReqVO req() {
        return new CrmReceivableSaveReqVO()
                .setOwnerUserId(OWNER_USER_ID)
                .setContractId(CONTRACT_ID)
                .setPrice(new BigDecimal("100.00"))
                .setReturnTime(LocalDateTime.of(2026, 10, 16, 10, 0))
                .setIdempotencyKey(IDEMPOTENCY_KEY);
    }

    private CrmReceivableDO winner(String payloadHash) {
        CrmReceivableDO winner = new CrmReceivableDO();
        winner.setId(WINNER_ID);
        winner.setNo("HK20261016000002");
        winner.setContractId(CONTRACT_ID);
        winner.setOwnerUserId(OWNER_USER_ID);
        winner.setPrice(new BigDecimal("100.00"));
        winner.setIdempotencyKey(IDEMPOTENCY_KEY);
        winner.setPayloadHash(payloadHash);
        return winner;
    }

    /**
     * 构造「走到 INSERT」所需的依赖桩。
     *
     * <p>两处预热检都返回 null，才可能走到 INSERT ——
     * 这正是真机上「赢家还没提交、普通读看不到它」的状态。</p>
     */
    private void stubForCreate() {
        CrmContractDO contract = new CrmContractDO();
        contract.setId(CONTRACT_ID);
        contract.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
        contract.setTotalPrice(new BigDecimal("100000.00"));
        when(contractMapper.selectByIdForUpdate(anyLong())).thenReturn(contract);
        when(receivableMapper.selectListByContractIdAndStatusForUpdate(anyLong(), any()))
                .thenReturn(java.util.Collections.emptyList());
        // validateRelationDataExists 走的是 contractService（不 mapper），
        // 返回 null 会让 getAuditStatus() 直接 NPE —— 桩必须补齐
        CrmContractService contractService = mock(CrmContractService.class);
        ReflectionTestUtils.setField(service, "contractService", contractService);
        when(contractService.validateContract(eq(CONTRACT_ID))).thenReturn(contract);
    }

    /**
     * 按服务端同套公式算 payloadHash。
     *
     * <p>公式必须与 {@code CrmReceivableServiceImpl#buildReceivablePayloadHash} 保持一致，
     * 所以用反射调它，而不是在这里重抄一份——重抄的那份一旦服务端改了就静默失配，
     * 而失配的表现恰好是「幂等被误判成同键不同载荷」，
     * 也就是把一次正常的幂等返回变成报错，这类测试骗人的事 ERP 侧已经踩过一次。</p>
     */
    private String payloadHashOf(CrmReceivableSaveReqVO reqVO) {
        return ReflectionTestUtils.invokeMethod(service, "buildReceivablePayloadHash", reqVO);
    }
}
