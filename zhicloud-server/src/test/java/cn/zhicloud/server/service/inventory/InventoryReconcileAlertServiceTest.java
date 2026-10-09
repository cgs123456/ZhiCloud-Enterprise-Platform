package cn.zhicloud.server.service.inventory;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.inventory.service.InventoryReconcileReport;
import cn.zhicloud.framework.inventory.service.InventoryReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryReconcileAlertServiceTest {

    private final InventoryReconciliationService reconciliationService =
            mock(InventoryReconciliationService.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final Environment environment = mock(Environment.class);
    private final InventoryReconcileAlertService service =
            new InventoryReconcileAlertService(reconciliationService, restTemplate, environment);

    @Test
    void consistentReportDoesNotCallWebhook() {
        InventoryReconcileReport report = new InventoryReconcileReport();
        report.setSourceCount(2);
        report.setProjectionCount(2);
        when(reconciliationService.reconcile()).thenReturn(report);

        InventoryReconcileAlertResult result = service.pushAlertIfNeeded();

        assertTrue(result.isConsistent());
        assertFalse(result.isPushed());
        assertEquals(0, result.getMismatchCount());
        verify(restTemplate, never()).exchange(any(URI.class), any(), any(), eq(String.class));
    }

    @Test
    void inconsistentReportWithoutWebhookFailsClosed() {
        when(reconciliationService.reconcile()).thenReturn(inconsistentReport());
        when(environment.getProperty(InventoryReconcileAlertService.WEBHOOK_URL_ENV)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, service::pushAlertIfNeeded);

        assertEquals(502, ex.getCode());
        assertTrue(ex.getMessage().contains(InventoryReconcileAlertService.WEBHOOK_URL_ENV));
    }

    @Test
    void inconsistentReportPostsManualAlertToWebhook() {
        when(reconciliationService.reconcile()).thenReturn(inconsistentReport());
        when(environment.getProperty(InventoryReconcileAlertService.WEBHOOK_URL_ENV))
                .thenReturn("https://alert.example.test/hook");
        when(environment.getProperty(InventoryReconcileAlertService.WEBHOOK_TOKEN_ENV))
                .thenReturn("test-token");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.ok("{}"));

        InventoryReconcileAlertResult result = service.pushAlertIfNeeded();

        assertFalse(result.isConsistent());
        assertTrue(result.isPushed());
        assertEquals(1, result.getMismatchCount());
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class));
    }

    private InventoryReconcileReport inconsistentReport() {
        InventoryReconcileReport report = new InventoryReconcileReport();
        report.setSourceCount(1);
        report.setProjectionCount(1);
        InventoryReconcileReport.Mismatch mismatch = new InventoryReconcileReport.Mismatch();
        mismatch.setSource("ERP");
        mismatch.setItemId(1L);
        mismatch.setWarehouseId(2L);
        mismatch.setTruthQuantity(BigDecimal.TEN);
        mismatch.setProjectionQuantity(BigDecimal.ONE);
        report.getMismatches().add(mismatch);
        return report;
    }

}
