package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
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
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §12.247（A 组第 4 项）：CRM 回款幂等兜底的死锁/退避路径测试（mapper 全 mock）。
 *
 * <p>为什么要 mock mapper：DeadlockLoserDataAccessException 只能由真实 InnoDB 成环产生，
 * H2 无法制造；本类钉的是「catch 收到死锁后如何裁决」的语义，不是数据库行为。
 * 真机侧由 Temp/zc_crm_receivable_concurrency_probe.py 证明无回归（§12.233.12 同款）。</p>
 *
 * @author 智云
 */
@Import(CrmReceivableServiceImpl.class)
public class CrmReceivableIdempotentWaitTest extends BaseDbUnitTest {

    @Resource
    private CrmReceivableServiceImpl receivableService;

    @MockitoBean
    private CrmReceivableMapper receivableMapper;
    @MockitoBean
    private CrmContractMapper contractMapper;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;
    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmReceivablePlanService receivablePlanService;
    @MockitoBean
    private CrmPermissionService permissionService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    private CrmReceivableDO winner;

    @BeforeEach
    public void setUp() {
        // 退避压到 0：语义与时序无关，只测「回查几次之后仍能裁决」
        ReflectionTestUtils.setField(receivableService, "idempotentWaitStepMs", 0L);
        ReflectionTestUtils.setField(receivableService, "idempotentWaitMaxMs", 200L);

        CrmContractDO contract = new CrmContractDO();
        contract.setId(1L);
        contract.setTotalPrice(new BigDecimal("1000.00"));
        contract.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
        when(contractMapper.selectByIdForUpdate(1L)).thenReturn(contract);
        when(contractService.validateContract(1L)).thenReturn(contract);
        when(receivableMapper.selectListByContractIdAndStatusForUpdate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(receivableMapper.selectByIdempotencyKey(anyString())).thenReturn(null);
        when(receivableMapper.selectByNo(anyString())).thenReturn(null);
        when(noRedisDAO.generate(anyString())).thenReturn("RECEIVABLE-DEADLOCK-001");

        winner = new CrmReceivableDO();
        winner.setId(9527L);
        winner.setIdempotencyKey("req-deadlock-1");
        winner.setPayloadHash(PayloadProbe.hash(receivableService, req("req-deadlock-1")));
    }

    private CrmReceivableSaveReqVO req(String key) {
        return new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(1L)
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 10, 17, 10, 0))
                .setIdempotencyKey(key);
    }

    @Test
    public void test_deadlockOnInsert_winnerBecomesVisibleAfterRetries() {
        DeadlockLoserDataAccessException deadlock =
                new DeadlockLoserDataAccessException("deadlock", new RuntimeException("sim"));
        org.mockito.Mockito.doThrow(deadlock).when(receivableMapper).insert(any(CrmReceivableDO.class));
        // 前两次回查赢家尚未提交，第三次可见
        when(receivableMapper.selectByIdempotencyKeyForUpdate("req-deadlock-1"))
                .thenReturn(null, null, winner);

        Long id = receivableService.createReceivable(req("req-deadlock-1"));

        assertEquals(9527L, id);
        verify(receivableMapper, times(3)).selectByIdempotencyKeyForUpdate("req-deadlock-1");
    }

    @Test
    public void test_deadlockOnInsert_winnerNeverVisible_throwsOriginalDeadlock() {
        DeadlockLoserDataAccessException deadlock =
                new DeadlockLoserDataAccessException("deadlock", new RuntimeException("sim"));
        org.mockito.Mockito.doThrow(deadlock).when(receivableMapper).insert(any(CrmReceivableDO.class));
        when(receivableMapper.selectByIdempotencyKeyForUpdate(anyString())).thenReturn(null);

        DeadlockLoserDataAccessException thrown = assertThrows(DeadlockLoserDataAccessException.class,
                () -> receivableService.createReceivable(req("req-deadlock-1")));
        assertEquals(deadlock, thrown, "必须抛最初那个异常，保留失败上下文");
    }

    @Test
    public void test_lockReadItselfDeadlocks_thenRecovers() {
        org.mockito.Mockito.doThrow(new DuplicateKeyException("dup")).when(receivableMapper).insert(any(CrmReceivableDO.class));
        DeadlockLoserDataAccessException readDeadlock =
                new DeadlockLoserDataAccessException("deadlock on read", new RuntimeException("sim"));
        // 回查第一次自己被选为牺牲品，第二次拿到赢家
        when(receivableMapper.selectByIdempotencyKeyForUpdate("req-deadlock-1"))
                .thenThrow(readDeadlock).thenReturn(winner);

        Long id = receivableService.createReceivable(req("req-deadlock-1"));

        assertEquals(9527L, id);
    }

    @Test
    public void test_winnerWithDifferentPayload_isRejected() {
        org.mockito.Mockito.doThrow(new DeadlockLoserDataAccessException("deadlock", new RuntimeException("sim")))
                .when(receivableMapper).insert(any(CrmReceivableDO.class));
        CrmReceivableDO other = new CrmReceivableDO();
        other.setId(1L);
        other.setIdempotencyKey("req-deadlock-1");
        other.setPayloadHash("0000000000000000000000000000000000000000000000000000000000000000");
        when(receivableMapper.selectByIdempotencyKeyForUpdate("req-deadlock-1")).thenReturn(other);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.createReceivable(req("req-deadlock-1")));
        assertEquals(1_020_004_011, exception.getCode());
    }

    /** 避免直接反射私有方法：测试包内可见的同包钩子不存在时退回反射计算 */
    static class PayloadProbe {
        static String hash(CrmReceivableServiceImpl service, CrmReceivableSaveReqVO reqVO) {
            return ReflectionTestUtils.invokeMethod(service, "buildReceivablePayloadHash", reqVO);
        }
    }
}
