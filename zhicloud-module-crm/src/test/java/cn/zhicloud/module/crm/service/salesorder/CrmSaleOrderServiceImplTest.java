package cn.zhicloud.module.crm.service.salesorder;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link CrmSaleOrderServiceImpl} 的单元测试：销售订单生命周期状态流转（CAS）
 *
 * 覆盖业务断点 3（§12.241）：confirm/ship/complete/cancel 四类迁移的出口与并发语义
 * （不复活已取消订单、重复动作不重复生效）。
 *
 * <p>覆盖 §12.261：submit 接 BPM（claim-first，失败不建实例）、审批回写驱动（approve=确认，
 * reject/撤回只动审批维度）、审批守卫矩阵（confirm/cancel/update/delete）。
 */
@Import(CrmSaleOrderServiceImpl.class)
public class CrmSaleOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmSaleOrderServiceImpl saleOrderService;

    @Resource
    private CrmSaleOrderMapper saleOrderMapper;

    @Resource
    private CrmContractMapper contractMapper;

    @Resource
    private CrmReceivableMapper receivableMapper;

    @MockitoBean
    private CrmCustomerService customerService;
    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;
    @MockitoBean
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Test
    public void test_confirmSaleOrder_success() {
        // mock 数据
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        // 调用
        saleOrderService.confirmSaleOrder(order.getId());
        // 校验
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_confirmSaleOrder_failNotDraft() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT);
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_confirmSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.confirmSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_shipSaleOrder_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        saleOrderService.shipSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.SHIPPED.getStatus(), result.getStatus());
        assertEquals(CrmSaleOrderDeliveryStatusEnum.SHIPPED.getStatus(), result.getDeliveryStatus());
    }

    @Test
    public void test_shipSaleOrder_failNotConfirmed() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        assertServiceException(() -> saleOrderService.shipSaleOrder(order.getId()),
                SALE_ORDER_SHIP_FAIL_NOT_CONFIRMED);
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_shipSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.shipSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_completeSaleOrder_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.SHIPPED.getStatus());
        saleOrderService.completeSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.COMPLETED.getStatus(), result.getStatus());
        assertEquals(CrmSaleOrderDeliveryStatusEnum.SIGNED.getStatus(), result.getDeliveryStatus());
    }

    @Test
    public void test_completeSaleOrder_failNotShipped() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        assertServiceException(() -> saleOrderService.completeSaleOrder(order.getId()),
                SALE_ORDER_COMPLETE_FAIL_NOT_SHIPPED);
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_completeSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.completeSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_cancelSaleOrder_fromDraft_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_fromConfirmed_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(), result.getStatus());
        // 取消不动发货/付款状态
        assertEquals(CrmSaleOrderDeliveryStatusEnum.UN_SHIPPED.getStatus(), result.getDeliveryStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(), result.getPaymentStatus());
    }

    @Test
    public void test_cancelSaleOrder_failAfterShipped() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.SHIPPED.getStatus());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
        assertEquals(CrmSaleOrderStatusEnum.SHIPPED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_failAfterCompleted() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.COMPLETED.getStatus());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
    }

    @Test
    public void test_cancelSaleOrder_twice_fail() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.cancelSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_confirmAfterCancel_notResurrect() {
        // 取消后确认必须失败，且状态保持已取消（CAS 语义：不复活终态订单）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT);
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    private CrmSaleOrderDO insertOrder(Integer status) {
        return insertOrder(status, CrmAuditStatusEnum.DRAFT.getStatus());
    }

    private CrmSaleOrderDO insertOrder(Integer status, Integer auditStatus) {
        CrmSaleOrderDO order = randomPojo(CrmSaleOrderDO.class, o -> {
            o.setStatus(status);
            o.setAuditStatus(auditStatus);
            o.setDeliveryStatus(CrmSaleOrderDeliveryStatusEnum.UN_SHIPPED.getStatus());
            o.setPaymentStatus(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus());
            o.setDeleted(false);
        });
        saleOrderMapper.insert(order);
        return order;
    }

    // ===== §12.260：创建时按合同收款进度初始化付款状态（投影） =====

    @Test
    public void test_createSaleOrder_paymentStatusByContractProgress_partial() {
        // 合同 1000 已审批回款 600 ⇒ 创建即部分付款
        CrmContractDO contract = insertApprovedContract("1000.00");
        insertApprovedReceivable(contract.getId(), "600.00");
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("SALE-ORDER-260-PARTIAL");

        Long orderId = saleOrderService.createSaleOrder(buildCreateReqVO(contract.getCustomerId(), contract.getId()), 1L);

        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                saleOrderMapper.selectById(orderId).getPaymentStatus());
    }

    @Test
    public void test_createSaleOrder_paymentStatusByContractProgress_paid() {
        // 合同 1000 已收齐 ⇒ 创建即已付款（先收齐后下单的投影语义）
        CrmContractDO contract = insertApprovedContract("1000.00");
        insertApprovedReceivable(contract.getId(), "1000.00");
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("SALE-ORDER-260-PAID");

        Long orderId = saleOrderService.createSaleOrder(buildCreateReqVO(contract.getCustomerId(), contract.getId()), 1L);

        assertEquals(CrmSaleOrderPaymentStatusEnum.PAID.getStatus(),
                saleOrderMapper.selectById(orderId).getPaymentStatus());
    }

    @Test
    public void test_createSaleOrder_paymentStatusByContractProgress_unpaidNoReceipts() {
        // 合同无可审批回款 ⇒ 未付款（与旧行为一致）
        CrmContractDO contract = insertApprovedContract("1000.00");
        when(contractService.validateContract(contract.getId())).thenReturn(contract);
        when(noRedisDAO.generate(anyString())).thenReturn("SALE-ORDER-260-UNPAID");

        Long orderId = saleOrderService.createSaleOrder(buildCreateReqVO(contract.getCustomerId(), contract.getId()), 1L);

        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                saleOrderMapper.selectById(orderId).getPaymentStatus());
    }

    @Test
    public void test_createSaleOrder_paymentStatus_unpaidWithoutContract() {
        // 无合同订单恒未付款
        when(noRedisDAO.generate(anyString())).thenReturn("SALE-ORDER-260-NOCONTRACT");

        Long orderId = saleOrderService.createSaleOrder(buildCreateReqVO(randomLongId(), null), 1L);

        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                saleOrderMapper.selectById(orderId).getPaymentStatus());
    }

    private CrmContractDO insertApprovedContract(String totalPrice) {
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setTotalPrice(new BigDecimal(totalPrice));
        });
        contractMapper.insert(contract);
        return contract;
    }

    private void insertApprovedReceivable(Long contractId, String price) {
        CrmReceivableDO receivable = randomPojo(CrmReceivableDO.class, o -> {
            o.setContractId(contractId);
            o.setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
            o.setPrice(new BigDecimal(price));
        });
        receivableMapper.insert(receivable);
    }

    private CrmSaleOrderSaveReqVO buildCreateReqVO(Long customerId, Long contractId) {
        return new CrmSaleOrderSaveReqVO()
                .setCustomerId(customerId)
                .setContractId(contractId)
                .setOrderDate(LocalDateTime.now())
                .setOwnerUserId(1L)
                .setItems(List.of(new CrmSaleOrderSaveReqVO.Item()
                        .setProductId(1L).setQuantity(BigDecimal.ONE).setUnitPrice(new BigDecimal("100.00"))));
    }

    // ===== §12.261：submit 接 BPM 审批（claim-first）+ 审批回写 + 审批守卫 =====

    @Test
    public void test_submitSaleOrder_success() {
        // 草稿 + 未提交 -> 抢占审批中 + 创建流程实例 + 回写流程号
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        when(bpmProcessInstanceApi.createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("PROC-261");
        // 调用
        saleOrderService.submitSaleOrder(order.getId(), 900100L);
        // 校验：审批中 + 业务状态不动 + 流程号落库 + BPM 入参（定义 KEY 与业务键）
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(), result.getAuditStatus());
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(), result.getStatus());
        assertEquals("PROC-261", result.getProcessInstanceId());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> captor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(900100L), captor.capture());
        assertEquals("crm-sale-order-audit", captor.getValue().getProcessDefinitionKey());
        assertEquals(String.valueOf(order.getId()), captor.getValue().getBusinessKey());
    }

    @Test
    public void test_submitSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.submitSaleOrder(randomLongId(), 1L),
                SALE_ORDER_NOT_EXISTS);
        verifyNoInteractions(bpmProcessInstanceApi);
    }

    @Test
    public void test_submitSaleOrder_failNotDraft() {
        // 已确认（未提交过审批）的订单：status 守卫 009，且绝不调 BPM
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        assertServiceException(() -> saleOrderService.submitSaleOrder(order.getId(), 1L),
                SALE_ORDER_SUBMIT_FAIL_STATUS_NOT_DRAFT);
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getAuditStatus());
        verifyNoInteractions(bpmProcessInstanceApi);
    }

    @Test
    public void test_submitSaleOrder_failAlreadyProcess() {
        // 审批中重复提交：audit 守卫 004，且不重复建实例
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        assertServiceException(() -> saleOrderService.submitSaleOrder(order.getId(), 1L),
                SALE_ORDER_SUBMIT_FAIL_NOT_DRAFT);
        verifyNoInteractions(bpmProcessInstanceApi);
    }

    @Test
    public void test_submitSaleOrder_failRejected() {
        // 驳回后不可重新提交（模块级现状，§12.261 D2/D9）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus());
        assertServiceException(() -> saleOrderService.submitSaleOrder(order.getId(), 1L),
                SALE_ORDER_SUBMIT_FAIL_NOT_DRAFT);
        verifyNoInteractions(bpmProcessInstanceApi);
    }

    @Test
    public void test_confirmSaleOrder_failAuditProcess() {
        // 审批中不可直接确认（由流程定局）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_AUDIT_STATUS);
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_confirmSaleOrder_failAuditRejected() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_AUDIT_STATUS);
    }

    @Test
    public void test_cancelSaleOrder_failAuditProcess() {
        // 审批中不可取消（须先撤回流程）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL_AUDIT_PROCESS);
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_deleteSaleOrder_failAuditProcess() {
        // 审批中不可删除（防悬垂运行实例）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        assertServiceException(() -> saleOrderService.deleteSaleOrder(order.getId()),
                SALE_ORDER_DELETE_FAIL_AUDIT_PROCESS);
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(),
                saleOrderMapper.selectById(order.getId()).getAuditStatus());
    }

    @Test
    public void test_deleteSaleOrder_okAfterReject() {
        // 驳回后删除是逃生口（§12.261 D4）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus());
        saleOrderService.deleteSaleOrder(order.getId());
        // 校验
        assertNull(saleOrderMapper.selectById(order.getId()));
    }

    @Test
    public void test_updateSaleOrder_failAuditRejected() {
        // 驳回/撤回后不可编辑（镜像回款编辑语义）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus());
        CrmSaleOrderSaveReqVO reqVO = buildCreateReqVO(randomLongId(), null).setId(order.getId());
        assertServiceException(() -> saleOrderService.updateSaleOrder(reqVO),
                SALE_ORDER_UPDATE_FAIL_EDITING_PROHIBITED);
    }

    @Test
    public void test_updateSaleOrder_okDuringAuditProcess() {
        // 审批中允许编辑（镜像回款编辑语义，§12.261 D4/D9）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        CrmSaleOrderSaveReqVO reqVO = buildCreateReqVO(randomLongId(), null).setId(order.getId());
        // 调用
        saleOrderService.updateSaleOrder(reqVO);
        // 校验：审批状态不被编辑影响
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(),
                saleOrderMapper.selectById(order.getId()).getAuditStatus());
    }

    @Test
    public void test_updateSaleOrderAuditStatus_approveDrivesConfirmed() {
        // 审批通过 = 订单确认（audit 10->20 且 status 10->20，同一条 CAS）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        // 调用
        saleOrderService.updateSaleOrderAuditStatus(order.getId(), BpmTaskStatusEnum.APPROVE.getStatus());
        // 校验
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(), result.getAuditStatus());
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(), result.getStatus());
    }

    @Test
    public void test_updateSaleOrderAuditStatus_rejectStaysDraft() {
        // 驳回只动审批维度，业务状态留在草稿
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        // 调用
        saleOrderService.updateSaleOrderAuditStatus(order.getId(), BpmTaskStatusEnum.REJECT.getStatus());
        // 校验
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmAuditStatusEnum.REJECT.getStatus(), result.getAuditStatus());
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(), result.getStatus());
    }

    @Test
    public void test_updateSaleOrderAuditStatus_cancelMaps40() {
        // 撤回（BPM CANCEL=4）必须映射为 CRM 已取消 40（§12.261 D6 修复钉）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus());
        // 调用
        saleOrderService.updateSaleOrderAuditStatus(order.getId(), BpmTaskStatusEnum.CANCEL.getStatus());
        // 校验
        assertEquals(CrmAuditStatusEnum.CANCEL.getStatus(),
                saleOrderMapper.selectById(order.getId()).getAuditStatus());
    }

    @Test
    public void test_updateSaleOrderAuditStatus_failNotProcess() {
        // 非审批中（未提交）不接受回写
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        assertServiceException(
                () -> saleOrderService.updateSaleOrderAuditStatus(order.getId(), BpmTaskStatusEnum.APPROVE.getStatus()),
                SALE_ORDER_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_updateSaleOrderAuditStatus_notExists() {
        assertServiceException(
                () -> saleOrderService.updateSaleOrderAuditStatus(randomLongId(), BpmTaskStatusEnum.APPROVE.getStatus()),
                SALE_ORDER_NOT_EXISTS);
    }
}
