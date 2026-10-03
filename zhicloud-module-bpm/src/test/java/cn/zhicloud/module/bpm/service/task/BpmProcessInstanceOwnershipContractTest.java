package cn.zhicloud.module.bpm.service.task;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import cn.zhicloud.module.bpm.dal.mysql.task.BpmProcessInstanceIdempotencyMapper;
import cn.zhicloud.module.bpm.enums.ErrorCodeConstants;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V118/V121 幂等登记 ownership 判定契约。
 *
 * <p>已接受的设计约束：对同一 (tenant_id, process_definition_key, business_key) 只保证单一 winner，
 * 且 ownership 只由当前读 {@code SELECT ... FOR UPDATE} 返回的 claim_token 决定，
 * 绝不依赖 {@code INSERT ... ON DUPLICATE KEY UPDATE} 的 affected rows 或是否抛出异常——
 * MySQL REPEATABLE-READ 下重复命中既不抛 DuplicateKeyException，
 * 在 useAffectedRows=false 默认值下也返回 affected rows 1，两个判定输入都不可靠。
 * 这是对已记录一致性读缺陷的显式接受，不是修复；R-011/R-029 仍为 OPEN。
 *
 * <p>本类只断言"归属从何而来"这一决策逻辑。它不证明 MySQL REPEATABLE-READ 行为本身，
 * 该行为只有隔离 MySQL 探针证据覆盖，且不是生产等价验证。
 */
class BpmProcessInstanceOwnershipContractTest {

    private static final Long TENANT_ID = 7L;
    private static final String KEY = "crm-receivable-audit";
    private static final String BUSINESS_KEY = "1";

    private BpmProcessInstanceIdempotencyMapper mapper;
    private BpmProcessDefinitionService processDefinitionService;
    private BpmProcessInstanceServiceImpl service;

    /** 由抢占调用捕获，用于让当前读返回"本次调用自己的"登记行。 */
    private String claimedToken;

    @BeforeEach
    void setUp() {
        mapper = mock(BpmProcessInstanceIdempotencyMapper.class);
        processDefinitionService = mock(BpmProcessDefinitionService.class);
        service = new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(service, "processInstanceIdempotencyMapper", mapper);
        ReflectionTestUtils.setField(service, "processDefinitionService", processDefinitionService);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 结构守卫：抢占方法返回 void，所以任何人都不可能把 ownership 判定改回 affected rows。
     */
    @Test
    void claimMethodCannotReportAffectedRows() throws NoSuchMethodException {
        Method claim = BpmProcessInstanceIdempotencyMapper.class
                .getMethod("insertOrLockUniqueKey", BpmProcessInstanceIdempotencyDO.class);
        assertEquals(void.class, claim.getReturnType());
    }

    /**
     * 当前读返回他人 token 时，本次调用必须判为输家：抛 PENDING，且绝不回填 process_instance_id。
     */
    @Test
    void loserIsDecidedByCurrentReadTokenAndNeverBackfills() {
        // 普通一致性读看不到 winner 已提交行，正是该缺陷的成因；只有当前读能拿到真实归属。
        when(mapper.selectByUniqueKey(TENANT_ID, KEY, BUSINESS_KEY)).thenReturn(null);
        when(mapper.selectByUniqueKeyForUpdate(TENANT_ID, KEY, BUSINESS_KEY))
                .thenReturn(registration("token-A", null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createProcessInstance(1L, createReq()));

        assertEquals(ErrorCodeConstants.PROCESS_INSTANCE_IDEMPOTENCY_PENDING.getCode(), ex.getCode());
        verify(mapper).insertOrLockUniqueKey(any(BpmProcessInstanceIdempotencyDO.class));
        verify(mapper, never()).updateById(any(BpmProcessInstanceIdempotencyDO.class));
    }

    /**
     * 接受该约束的代价：空白登记（含 legacy NULL token）会走无锁快路径持续 fail-closed，
     * 不回填、不重试抢占，也不猜测归属。
     */
    @Test
    void blankRegistrationFailsClosedWithoutGuessingOwnership() {
        when(mapper.selectByUniqueKey(TENANT_ID, KEY, BUSINESS_KEY))
                .thenReturn(registration(null, null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createProcessInstance(1L, createReq()));

        assertEquals(ErrorCodeConstants.PROCESS_INSTANCE_IDEMPOTENCY_PENDING.getCode(), ex.getCode());
        verify(mapper, never()).insertOrLockUniqueKey(any(BpmProcessInstanceIdempotencyDO.class));
        verify(mapper, never()).updateById(any(BpmProcessInstanceIdempotencyDO.class));
    }

    /**
     * 契约的另一半：当前读返回本次调用自己的 token 时，必须被判为 winner 并继续创建，
     * 而不是误报 PENDING。用"流程定义不存在"这一后续业务异常来证明已经越过归属判定关口。
     */
    @Test
    void ownTokenFromCurrentReadIsWinnerAndAdvancesPastOwnershipGate() {
        when(mapper.selectByUniqueKey(TENANT_ID, KEY, BUSINESS_KEY)).thenReturn(null);
        doAnswer(invocation -> {
            claimedToken = ((BpmProcessInstanceIdempotencyDO) invocation.getArgument(0)).getClaimToken();
            return null;
        }).when(mapper).insertOrLockUniqueKey(any(BpmProcessInstanceIdempotencyDO.class));
        when(mapper.selectByUniqueKeyForUpdate(TENANT_ID, KEY, BUSINESS_KEY))
                .thenAnswer(invocation -> registration(claimedToken, null));
        when(processDefinitionService.getActiveProcessDefinition(KEY)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createProcessInstance(1L, createReq()));

        assertNotEquals(ErrorCodeConstants.PROCESS_INSTANCE_IDEMPOTENCY_PENDING.getCode(), ex.getCode());
        verify(mapper).insertOrLockUniqueKey(any(BpmProcessInstanceIdempotencyDO.class));
    }

    private BpmProcessInstanceIdempotencyDO registration(String claimToken, String processInstanceId) {
        BpmProcessInstanceIdempotencyDO row = new BpmProcessInstanceIdempotencyDO();
        row.setTenantId(TENANT_ID);
        row.setProcessDefinitionKey(KEY);
        row.setBusinessKey(BUSINESS_KEY);
        row.setClaimToken(claimToken);
        row.setProcessInstanceId(processInstanceId);
        return row;
    }

    private BpmProcessInstanceCreateReqDTO createReq() {
        return new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(KEY)
                .setBusinessKey(BUSINESS_KEY);
    }
}
