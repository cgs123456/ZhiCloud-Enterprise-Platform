package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * {@link CrmReceivableServiceImpl} 的单元测试。
 */
@Import(CrmReceivableServiceImpl.class)
public class CrmReceivableServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmReceivableServiceImpl receivableService;

    @Resource
    private CrmReceivableMapper receivableMapper;

    @Resource
    private CrmContractMapper contractMapper;

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

    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;

    @Test
    public void test_createReceivable_draftOccupiesContractAmount() {
        // 准备：合同总额 100，草稿回款已占用 60
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("100.00"));
        });
        contractMapper.insert(contract);
        CrmReceivableDO draft = randomPojo(CrmReceivableDO.class, o -> {
            o.setContractId(contract.getId());
            o.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
            o.setPrice(new BigDecimal("60.00"));
        });
        receivableMapper.insert(draft);
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("RECEIVABLE-001");

        // 调用：再创建 50，超过剩余额度 40
        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("50.00"))
                .setReturnTime(LocalDateTime.now())
                .setIdempotencyKey("req-exceeds-limit");
        assertThrows(ServiceException.class, () -> receivableService.createReceivable(reqVO));
    }

    @Test
    public void test_createReceivable_nullContractAmountFailsClosed() {
        // 准备：合同总额缺失时不得按 0 元或空值继续创建
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(null);
        });
        contractMapper.insert(contract);
        when(contractService.validateContract(contract.getId())).thenReturn(contract);

        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.now())
                .setIdempotencyKey("req-null-contract-amount");
        assertThrows(ServiceException.class, () -> receivableService.createReceivable(reqVO));
    }



    @Test
    public void test_createReceivable_sameIdempotencyKeySamePayloadReturnsOriginalId() {
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("100.00"));
        });
        contractMapper.insert(contract);
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("RECEIVABLE-IDEMPOTENT-001");

        CrmReceivableSaveReqVO first = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("client-request-001");
        Long firstId = receivableService.createReceivable(first);

        CrmReceivableSaveReqVO retry = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("10.0000"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("client-request-001");
        Long retryId = receivableService.createReceivable(retry);

        assertEquals(firstId, retryId);
        assertEquals(1L, receivableMapper.selectCount(null));
    }

    @Test
    public void test_createReceivable_sameIdempotencyKeyDifferentPayloadFails() {
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("100.00"));
        });
        contractMapper.insert(contract);
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("RECEIVABLE-IDEMPOTENT-002");

        CrmReceivableSaveReqVO first = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("client-request-002");
        receivableService.createReceivable(first);

        CrmReceivableSaveReqVO changed = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("11.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("client-request-002");
        assertThrows(ServiceException.class, () -> receivableService.createReceivable(changed));
        assertEquals(1L, receivableMapper.selectCount(null));
    }

    @Test
    public void test_createReceivable_blankIdempotencyKeyFails() {
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("100.00"));
        });
        contractMapper.insert(contract);

        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("   ");
        assertThrows(ServiceException.class, () -> receivableService.createReceivable(reqVO));
        assertEquals(0L, receivableMapper.selectCount(null));
    }

    @Test
    public void test_createReceivable_nullPriceFailsClosed() {
        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(1L)
                .setPrice(null)
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("req-null-price");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.createReceivable(reqVO));
        assertEquals(1_020_004_013, exception.getCode());
        assertEquals(0L, receivableMapper.selectCount(null));
    }

    @Test
    public void test_createReceivable_nonPositivePriceFailsClosed() {
        for (BigDecimal price : java.util.List.of(BigDecimal.ZERO, new BigDecimal("-0.01"))) {
            CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                    .setOwnerUserId(1L)
                    .setContractId(1L)
                    .setPrice(price)
                    .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                    .setIdempotencyKey("req-non-positive-" + price);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> receivableService.createReceivable(reqVO));
            assertEquals(1_020_004_013, exception.getCode());
        }
        assertEquals(0L, receivableMapper.selectCount(null));
    }

    @Test
    public void test_createReceivable_priceScaleExceedsTwoFailsClosed() {
        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(1L)
                .setPrice(new BigDecimal("10.001"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("req-price-scale");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.createReceivable(reqVO));
        assertEquals(1_020_004_014, exception.getCode());
        assertEquals(0L, receivableMapper.selectCount(null));
    }


    @Test
    public void test_createReceivable_planContractMismatchFailsClosed() {
        // 准备：合同已审批，但回款计划属于另一份合同
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("100.00"));
        });
        contractMapper.insert(contract);
        CrmReceivablePlanDO plan = randomPojo(CrmReceivablePlanDO.class, o -> {
            o.setContractId(contract.getId() + 1);
            o.setCustomerId(contract.getCustomerId());
            o.setReceivableId(null);
        });
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(receivablePlanService.getReceivablePlan(plan.getId())).thenReturn(plan);

        CrmReceivableSaveReqVO reqVO = new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(contract.getId())
                .setPlanId(plan.getId())
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0))
                .setIdempotencyKey("req-plan-contract-mismatch");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.createReceivable(reqVO));
        assertEquals(1_020_005_001, exception.getCode());
        assertEquals(0L, receivableMapper.selectCount(null));
    }
}