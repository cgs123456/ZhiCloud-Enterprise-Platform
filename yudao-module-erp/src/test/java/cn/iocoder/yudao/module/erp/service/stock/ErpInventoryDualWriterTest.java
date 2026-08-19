package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ErpInventoryDualWriter 单元测试（M2 阶段 B）
 *
 * @author 智云库存治理
 */
@ExtendWith(MockitoExtension.class)
public class ErpInventoryDualWriterTest {

    @Mock
    private InventoryService inventoryService;

    private ErpInventoryDualWriter writer;

    @BeforeEach
    void init() {
        writer = new ErpInventoryDualWriter();
        // 通过 Spring 反射注入 mock（避免 Spring 上下文依赖）
        ReflectionTestUtils.setField(writer, "inventoryService", inventoryService);
    }

    @Test
    void dualWriteIgnoresZeroQuantityDelta() {
        writer.dualWrite(1L, 2L, null, null, null, null, BigDecimal.ZERO, null);
        verify(inventoryService, never()).add(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void dualWriteCallsAddForPositiveQuantityDelta() {
        writer.dualWrite(10L, 20L, null, null, null, null, new BigDecimal("5"), null);

        ArgumentCaptor<Long> itemId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> warehouseId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> batchCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> delta = ArgumentCaptor.forClass(BigDecimal.class);

        verify(inventoryService).add(itemId.capture(), warehouseId.capture(),
                eq(null), eq(null), eq(null), batchCode.capture(), delta.capture());

        assertEquals(10L, itemId.getValue());
        assertEquals(20L, warehouseId.getValue());
        assertEquals(null, batchCode.getValue());
        assertEquals(new BigDecimal("5"), delta.getValue());
    }

    @Test
    void dualWriteCallsAddForNegativeQuantityDelta() {
        writer.dualWrite(10L, 20L, null, null, null, null, new BigDecimal("-3"), null);

        ArgumentCaptor<BigDecimal> delta = ArgumentCaptor.forClass(BigDecimal.class);
        verify(inventoryService).add(any(), any(), any(), any(), any(), any(), delta.capture());
        assertEquals(new BigDecimal("-3"), delta.getValue());
    }

    @Test
    void dualWriteCallsReserveForPositiveLockedDelta() {
        writer.dualWrite(10L, 20L, null, null, null, null, null, new BigDecimal("2"));

        verify(inventoryService).reserve(eq(10L), eq(20L), eq(null), eq(null), eq(null),
                argThat(delta -> delta != null && delta.compareTo(new BigDecimal("2")) == 0));
    }

    @Test
    void dualWriteCallsReleaseForNegativeLockedDelta() {
        writer.dualWrite(10L, 20L, null, null, null, null, null, new BigDecimal("-4"));

        verify(inventoryService).release(eq(10L), eq(20L), eq(null), eq(null), eq(null),
                argThat(delta -> delta != null && delta.compareTo(new BigDecimal("4")) == 0));
    }

    @Test
    void dualWritePropagatesExceptionAsRuntimeException() {
        when(inventoryService.add(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB 异常"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> writer.dualWrite(1L, 2L, null, null, null, null, new BigDecimal("1"), null));

        assertEquals("[ErpInventoryDualWriter] 双写 inventory_item quantity 失败，触发事务回滚", ex.getMessage());
    }

    @Test
    void dualWriteSkipsWhenBothDeltasAreZero() {
        writer.dualWrite(1L, 2L, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO);
        verify(inventoryService, never()).add(any(), any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).reserve(any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).release(any(), any(), any(), any(), any(), any());
    }

    @Test
    void dualWriteSkipsWhenQuantityNullAndLockedZero() {
        writer.dualWrite(1L, 2L, null, null, null, null, null, BigDecimal.ZERO);
        verify(inventoryService, never()).add(any(), any(), any(), any(), any(), any(), any());
    }
}
