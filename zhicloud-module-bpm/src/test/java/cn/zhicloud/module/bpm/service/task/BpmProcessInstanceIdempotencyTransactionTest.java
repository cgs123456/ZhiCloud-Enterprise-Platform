package cn.zhicloud.module.bpm.service.task;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import cn.zhicloud.module.bpm.dal.mysql.task.BpmProcessInstanceIdempotencyMapper;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmProcessInstanceIdempotencyTransactionTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 7L;

    @Resource
    private BpmProcessInstanceIdempotencyMapper idempotencyMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private BpmProcessInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        service = new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(service, "processInstanceIdempotencyMapper", idempotencyMapper);
        ReflectionTestUtils.setField(service, "processDefinitionService", mock(BpmProcessDefinitionService.class));
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createProcessInstanceRollsBackClaimWhenProcessDefinitionLookupFails() {
        BpmProcessDefinitionService processDefinitionService = (BpmProcessDefinitionService)
                ReflectionTestUtils.getField(service, "processDefinitionService");
        when(processDefinitionService.getActiveProcessDefinition("crm-receivable-audit"))
                .thenThrow(new IllegalStateException("flowable unavailable"));

        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(status ->
                service.createProcessInstance(1L, createReq())));

        assertNull(idempotencyMapper.selectByUniqueKey(TENANT_ID, "crm-receivable-audit", "1"));
    }

    @Test
    void manualClaimAndRollbackKeepsUniqueKeyReusable() {
        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            idempotencyMapper.insert(newClaim());
            throw new IllegalStateException("rollback claim");
        }));

        assertNull(idempotencyMapper.selectByUniqueKey(TENANT_ID, "crm-receivable-audit", "1"));
        Integer result = transactionTemplate.execute(status -> {
            idempotencyMapper.insert(newClaim());
            return 0;
        });
        assertEquals(0, result);
        assertNull(idempotencyMapper.selectByUniqueKey(TENANT_ID, "crm-receivable-audit", "1")
                .getProcessInstanceId());
    }

    private BpmProcessInstanceCreateReqDTO createReq() {
        return new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey("crm-receivable-audit")
                .setBusinessKey("1");
    }

    private BpmProcessInstanceIdempotencyDO newClaim() {
        BpmProcessInstanceIdempotencyDO claim = new BpmProcessInstanceIdempotencyDO();
        claim.setTenantId(TENANT_ID);
        claim.setProcessDefinitionKey("crm-receivable-audit");
        claim.setBusinessKey("1");
        return claim;
    }
}
