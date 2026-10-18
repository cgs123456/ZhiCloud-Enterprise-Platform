package cn.zhicloud.module.erp.service.integration;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.integration.contract.CrmReceivableApprovedEvent;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.service.finance.ErpFinanceReceiptService;
import cn.zhicloud.module.erp.service.finance.bo.ErpFinanceReceiptCrmCreateReqBO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_REQUIRED;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 「CRM 回款审批通过 -> ERP 收款单」消费者单元测试（D1/§12.267）
 *
 * <p>用例对应链路里最容易退化、且退化后最难发现的六个不变量：
 * 映射正确、重复投递不建第二张单、映射缺失必须失败进死信、回款日期缺失时兜底当前时间、
 * 租户缺失 fail-closed、消费者名与事件类型契约稳定。
 */
@ExtendWith(MockitoExtension.class)
class CrmReceivableApprovedConsumerTest {

    @Mock
    private ErpFinanceReceiptService financeReceiptService;
    @Mock
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Mock
    private MdmCrossRefService mdmCrossRefService;

    @InjectMocks
    private CrmReceivableApprovedConsumer consumer;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsReceiptWithMdmMappedCustomer() throws Exception {
        when(financeReceiptMapper.selectByIdempotencyKey("crm-receivable-1024")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(7L, "CRM", "CUSTOMER", "88", "ERP", "CUSTOMER"))
                .thenReturn(913000L);
        when(financeReceiptService.createFinanceReceiptFromCrm(any())).thenReturn(500L);

        consumer.handle(outboxEvent(payload()));

        ArgumentCaptor<ErpFinanceReceiptCrmCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpFinanceReceiptCrmCreateReqBO.class);
        verify(financeReceiptService).createFinanceReceiptFromCrm(captor.capture());
        ErpFinanceReceiptCrmCreateReqBO req = captor.getValue();
        assertEquals("crm-receivable-1024", req.getIdempotencyKey());
        assertEquals(1024L, req.getReceivableId());
        assertEquals("HK20261018000001", req.getReceivableNo());
        assertEquals(913000L, req.getCustomerId());
        assertEquals(LocalDateTime.of(2026, 10, 18, 10, 0), req.getReceiptTime());
        assertEquals(new BigDecimal("400.00"), req.getPrice());
    }

    @Test
    void skipsWhenReceiptAlreadyExists() throws Exception {
        when(financeReceiptMapper.selectByIdempotencyKey("crm-receivable-1024"))
                .thenReturn(new ErpFinanceReceiptDO().setId(9L));

        consumer.handle(outboxEvent(payload()));

        verify(financeReceiptService, never()).createFinanceReceiptFromCrm(any());
        // 幂等短路必须在最前面：连主数据映射都不该发生
        verifyNoInteractions(mdmCrossRefService);
    }

    @Test
    void failsWhenCustomerMappingMissing() throws Exception {
        when(financeReceiptMapper.selectByIdempotencyKey("crm-receivable-1024")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(7L, "CRM", "CUSTOMER", "88", "ERP", "CUSTOMER"))
                .thenThrow(exception(MDM_CROSS_REF_NOT_FOUND, "CRM", "CUSTOMER", "88", "ERP", "CUSTOMER"));

        ServiceException ex = assertThrows(ServiceException.class, () -> consumer.handle(outboxEvent(payload())));

        assertEquals(1_099_002_003, ex.getCode());
        // 映射缺失绝不能静默建单：挂到错误客户上的收款单会进错对账口径且事后难发现
        verify(financeReceiptService, never()).createFinanceReceiptFromCrm(any());
    }

    @Test
    void fallsBackToNowWhenReturnTimeMissing() throws Exception {
        when(financeReceiptMapper.selectByIdempotencyKey("crm-receivable-1024")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(7L, "CRM", "CUSTOMER", "88", "ERP", "CUSTOMER"))
                .thenReturn(913000L);
        when(financeReceiptService.createFinanceReceiptFromCrm(any())).thenReturn(500L);

        consumer.handle(outboxEvent(payloadWithoutReturnTime()));

        ArgumentCaptor<ErpFinanceReceiptCrmCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpFinanceReceiptCrmCreateReqBO.class);
        verify(financeReceiptService).createFinanceReceiptFromCrm(captor.capture());
        assertNotNull(captor.getValue().getReceiptTime());
    }

    @Test
    void failsClosedWhenEventTenantMissing() throws Exception {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> consumer.handle(outboxEvent(payload(), null)));

        assertEquals(EVENT_TENANT_REQUIRED.getCode(), ex.getCode());
        verifyNoInteractions(financeReceiptMapper, financeReceiptService, mdmCrossRefService);
    }

    @Test
    void handlesWithinEventTenantAndRestoresCallerContext() throws Exception {
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.setIgnore(true);
        when(financeReceiptMapper.selectByIdempotencyKey("crm-receivable-1024")).thenAnswer(invocation -> {
            assertEquals(7L, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            return null;
        });
        when(mdmCrossRefService.mapTargetIdAsLong(7L, "CRM", "CUSTOMER", "88", "ERP", "CUSTOMER"))
                .thenReturn(913000L);
        when(financeReceiptService.createFinanceReceiptFromCrm(any())).thenReturn(500L);

        consumer.handle(outboxEvent(payload()));

        assertEquals(7L, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    /**
     * 消费者名与事件类型是「契约」而不是实现细节：改名会让 Inbox 去重键变化，
     * 历史事件会被当成新消费者重新消费一次；事件类型写错则整条链路静默不生效。
     */
    @Test
    void declaresStableConsumerNameAndEventType() {
        assertEquals("erp-finance-receipt-creator", consumer.name());
        assertTrue(consumer.supports("crm.receivable.approved.v1"));
        assertFalse(consumer.supports("crm.receivable.approved.v2"));
        assertFalse(consumer.supports("erp.sale-out.approved.v1"));
    }

    private static EventOutboxDO outboxEvent(CrmReceivableApprovedEvent payload) {
        return outboxEvent(payload, 7L);
    }

    private static EventOutboxDO outboxEvent(CrmReceivableApprovedEvent payload, Long tenantId) {
        return EventOutboxDO.builder()
                .id(1L)
                .eventId(CrmReceivableApprovedEvent.eventIdOf(payload.getReceivableId()))
                .eventType("crm.receivable.approved.v1")
                .tenantId(tenantId)
                .payload(payload.toJson())
                .build();
    }

    private static CrmReceivableApprovedEvent payload() {
        return CrmReceivableApprovedEvent.builder()
                .no("HK20261018000001")
                .receivableId(1024L)
                .customerId(88L)
                .returnTime(LocalDateTime.of(2026, 10, 18, 10, 0))
                .price(new BigDecimal("400.00"))
                .build();
    }

    private static CrmReceivableApprovedEvent payloadWithoutReturnTime() {
        return CrmReceivableApprovedEvent.builder()
                .no("HK20261018000001")
                .receivableId(1024L)
                .customerId(88L)
                .price(new BigDecimal("400.00"))
                .build();
    }

}
