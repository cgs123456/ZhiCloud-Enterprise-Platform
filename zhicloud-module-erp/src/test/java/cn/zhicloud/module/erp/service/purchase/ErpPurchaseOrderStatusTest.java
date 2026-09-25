package cn.zhicloud.module.erp.service.purchase;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 采购订单审核状态行为测试。
 */
@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderStatusTest {

    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;

    private ErpPurchaseOrderServiceImpl purchaseOrderService;

    @BeforeEach
    void setUp() {
        purchaseOrderService = new ErpPurchaseOrderServiceImpl();
        ReflectionTestUtils.setField(purchaseOrderService, "purchaseOrderMapper", purchaseOrderMapper);
    }

    @Test
    void updateStatusRejectsUnknownStatusBeforeDatabaseAccess() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(1L, 999));

        assertTrue(exception.getMessage().contains("审核状态"));
        verifyNoInteractions(purchaseOrderMapper);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotUpdateAgain() {
        when(purchaseOrderMapper.selectById(1L)).thenReturn(new ErpPurchaseOrderDO()
                .setId(1L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));

        assertDoesNotThrow(() -> purchaseOrderService.updatePurchaseOrderStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseOrderMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any());
    }

    @Test
    void concurrentDuplicateApprovalReturnsSameResultWhenCasLoses() {
        when(purchaseOrderMapper.selectById(1L))
                .thenReturn(purchaseOrder(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseOrderMapper.selectOneForUpdate(any(), eq(1L)))
                .thenReturn(purchaseOrder(ErpAuditStatus.APPROVE.getStatus()));
        when(purchaseOrderMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(0);

        assertDoesNotThrow(() -> purchaseOrderService.updatePurchaseOrderStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseOrderMapper).selectOneForUpdate(any(), eq(1L));
    }

    @Test
    void concurrentConflictingStatusStillThrows() {
        when(purchaseOrderMapper.selectById(1L))
                .thenReturn(purchaseOrder(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseOrderMapper.selectOneForUpdate(any(), eq(1L)))
                .thenReturn(purchaseOrder(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseOrderMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(
                        1L, ErpAuditStatus.APPROVE.getStatus()));

        assertTrue(exception.getMessage().contains("审核失败"));
        verify(purchaseOrderMapper).selectOneForUpdate(any(), eq(1L));
    }

    private static ErpPurchaseOrderDO purchaseOrder(Integer status) {
        return new ErpPurchaseOrderDO().setId(1L).setStatus(status)
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
    }
}
