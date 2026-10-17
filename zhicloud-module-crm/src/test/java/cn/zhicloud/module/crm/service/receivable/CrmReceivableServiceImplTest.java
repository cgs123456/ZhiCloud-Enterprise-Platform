package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
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

    @Resource
    private CrmSaleOrderMapper saleOrderMapper;

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

    // ===== §12.260：回款审批通过驱动销售订单付款状态（合同收款进度投影） =====

    @Test
    public void test_updateReceivableAuditStatus_approveDrivesPartialPayment() {
        // 准备：合同总额 1000；订单 A(600)/B(400) + 已取消订单 C(200)；回款 400 审批中
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract);
        CrmSaleOrderDO orderA = insertSaleOrder(contract.getId(), "600.00", CrmSaleOrderStatusEnum.DRAFT.getStatus());
        CrmSaleOrderDO orderB = insertSaleOrder(contract.getId(), "400.00", CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        CrmSaleOrderDO orderC = insertSaleOrder(contract.getId(), "200.00", CrmSaleOrderStatusEnum.CANCELED.getStatus());
        CrmReceivableDO receivable = insertProcessingReceivable(contract.getId(), "400.00");

        // 调用：回款审批通过
        receivableService.updateReceivableAuditStatus(receivable.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        // 断言：回款 APPROVE；未取消订单部分付款；已取消订单不被驱动
        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(),
                receivableMapper.selectById(receivable.getId()).getAuditStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                saleOrderMapper.selectById(orderA.getId()).getPaymentStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                saleOrderMapper.selectById(orderB.getId()).getPaymentStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                saleOrderMapper.selectById(orderC.getId()).getPaymentStatus());
    }

    @Test
    public void test_updateReceivableAuditStatus_approveToFullDrivesPaid() {
        // 准备：合同总额 1000；已有审批通过回款 600；订单 A 已是部分付款；新回款 400 审批中
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract);
        insertReceivable(contract.getId(), "600.00", CrmAuditStatusEnum.APPROVE.getStatus());
        CrmSaleOrderDO orderA = insertSaleOrder(contract.getId(), "600.00", CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderMapper.updateById(new CrmSaleOrderDO().setId(orderA.getId())
                .setPaymentStatus(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus()));
        CrmReceivableDO second = insertReceivable(contract.getId(), "400.00", CrmAuditStatusEnum.PROCESS.getStatus());

        // 调用：第二笔审批通过 → 累计 1000 = 合同总额
        receivableService.updateReceivableAuditStatus(second.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        // 断言：订单升为已付款
        assertEquals(CrmSaleOrderPaymentStatusEnum.PAID.getStatus(),
                saleOrderMapper.selectById(orderA.getId()).getPaymentStatus());
    }

    @Test
    public void test_updateReceivableAuditStatus_rejectDoesNotDrive() {
        // 准备：合同总额 1000；订单 A；回款 400 审批中
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract);
        CrmSaleOrderDO orderA = insertSaleOrder(contract.getId(), "600.00", CrmSaleOrderStatusEnum.DRAFT.getStatus());
        CrmReceivableDO receivable = insertProcessingReceivable(contract.getId(), "400.00");

        // 调用：审批驳回
        receivableService.updateReceivableAuditStatus(receivable.getId(), BpmTaskStatusEnum.REJECT.getStatus());

        // 断言：回款 REJECT；订单付款状态不动
        assertEquals(CrmAuditStatusEnum.REJECT.getStatus(),
                receivableMapper.selectById(receivable.getId()).getAuditStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                saleOrderMapper.selectById(orderA.getId()).getPaymentStatus());
    }

    @Test
    public void test_updateReceivableAuditStatus_approveWithoutOrdersOk() {
        // 合同下没有订单：投影空写 0 行，不抛错
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract);
        CrmReceivableDO receivable = insertProcessingReceivable(contract.getId(), "400.00");

        receivableService.updateReceivableAuditStatus(receivable.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(),
                receivableMapper.selectById(receivable.getId()).getAuditStatus());
    }

    @Test
    public void test_updateReceivableAuditStatus_missingContractSkipsProjection() {
        // 合同缺失（存在回款则合同不可删，理论不可达）：跳过投影、审批仍成功
        CrmReceivableDO receivable = insertProcessingReceivable(987654321L, "10.00");

        receivableService.updateReceivableAuditStatus(receivable.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(),
                receivableMapper.selectById(receivable.getId()).getAuditStatus());
    }

    @Test
    public void test_updateReceivableAuditStatus_otherContractUntouched() {
        // 合同 1 的订单不受合同 2 回款审批影响
        CrmContractDO contract1 = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract1);
        CrmContractDO contract2 = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal("1000.00"));
        });
        contractMapper.insert(contract2);
        CrmSaleOrderDO order1 = insertSaleOrder(contract1.getId(), "600.00", CrmSaleOrderStatusEnum.DRAFT.getStatus());
        CrmSaleOrderDO order2 = insertSaleOrder(contract2.getId(), "600.00", CrmSaleOrderStatusEnum.DRAFT.getStatus());
        CrmReceivableDO receivable = insertProcessingReceivable(contract2.getId(), "1000.00");

        receivableService.updateReceivableAuditStatus(receivable.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        // 合同 2 的订单已付款；合同 1 的订单不动
        assertEquals(CrmSaleOrderPaymentStatusEnum.PAID.getStatus(),
                saleOrderMapper.selectById(order2.getId()).getPaymentStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                saleOrderMapper.selectById(order1.getId()).getPaymentStatus());
    }

    private CrmSaleOrderDO insertSaleOrder(Long contractId, String finalAmount, Integer status) {
        CrmSaleOrderDO order = randomPojo(CrmSaleOrderDO.class, o -> {
            o.setContractId(contractId);
            o.setFinalAmount(new BigDecimal(finalAmount));
            o.setStatus(status);
            o.setPaymentStatus(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus());
        });
        saleOrderMapper.insert(order);
        return order;
    }

    private CrmReceivableDO insertReceivable(Long contractId, String price, Integer auditStatus) {
        CrmReceivableDO receivable = randomPojo(CrmReceivableDO.class, o -> {
            o.setContractId(contractId);
            o.setAuditStatus(auditStatus);
            o.setPrice(new BigDecimal(price));
        });
        receivableMapper.insert(receivable);
        return receivable;
    }

    private CrmReceivableDO insertProcessingReceivable(Long contractId, String price) {
        return insertReceivable(contractId, price, CrmAuditStatusEnum.PROCESS.getStatus());
    }
}