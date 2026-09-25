package cn.zhicloud.module.erp.service.purchase;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
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
 * 采购入库审核状态行为测试，重点验证重复请求不会重复记账。
 */
@ExtendWith(MockitoExtension.class)
class ErpPurchaseInStatusTest {

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpStockRecordService stockRecordService;

    private ErpPurchaseInServiceImpl purchaseInService;

    @BeforeEach
    void setUp() {
        purchaseInService = new ErpPurchaseInServiceImpl();
        ReflectionTestUtils.setField(purchaseInService, "purchaseInMapper", purchaseInMapper);
        ReflectionTestUtils.setField(purchaseInService, "purchaseInItemMapper", purchaseInItemMapper);
        ReflectionTestUtils.setField(purchaseInService, "stockRecordService", stockRecordService);
    }

    @Test
    void updateStatusRejectsUnknownStatusBeforeDatabaseAccess() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseInService.updatePurchaseInStatus(1L, 999));

        assertTrue(exception.getMessage().contains("审核状态"));
        verifyNoInteractions(purchaseInMapper, purchaseInItemMapper, stockRecordService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostStockAgain() {
        when(purchaseInMapper.selectById(1L)).thenReturn(purchaseIn(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any());
        verifyNoInteractions(purchaseInItemMapper, stockRecordService);
    }

    @Test
    void concurrentDuplicateApprovalReturnsWithoutPostingStockAgain() {
        when(purchaseInMapper.selectById(1L))
                .thenReturn(purchaseIn(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInMapper.selectOneForUpdate(any(), eq(1L)))
                .thenReturn(purchaseIn(ErpAuditStatus.APPROVE.getStatus()));
        when(purchaseInMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(0);

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseInMapper).selectOneForUpdate(any(), eq(1L));
        verifyNoInteractions(purchaseInItemMapper, stockRecordService);
    }

    private static ErpPurchaseInDO purchaseIn(Integer status) {
        return new ErpPurchaseInDO().setId(1L).setNo("PI001").setStatus(status)
                .setPaymentPrice(BigDecimal.ZERO);
    }
}
