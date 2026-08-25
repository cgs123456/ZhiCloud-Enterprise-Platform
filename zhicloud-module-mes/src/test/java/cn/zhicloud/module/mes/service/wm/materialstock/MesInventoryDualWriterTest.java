package cn.zhicloud.module.mes.service.wm.materialstock;

import cn.zhicloud.framework.inventory.service.InventoryService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MesInventoryDualWriter 单元测试（M2 阶段 B）
 *
 * @author 智云库存治理
 */
@ExtendWith(MockitoExtension.class)
public class MesInventoryDualWriterTest {

    @Mock
    private InventoryService inventoryService;

    private MesInventoryDualWriter writer;

    @BeforeEach
    void init() {
        writer = new MesInventoryDualWriter();
        ReflectionTestUtils.setField(writer, "inventoryService", inventoryService);
    }

    @Test
    void dualWriteIgnoresZeroQuantityDelta() {
        writer.dualWrite(1L, 2L, 3L, 4L, 5L, "B1", BigDecimal.ZERO, null);
        verify(inventoryService, never()).add(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void dualWriteIgnoresNullQuantityDelta() {
        writer.dualWrite(1L, 2L, 3L, 4L, 5L, "B1", null, null);
        verify(inventoryService, never()).add(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void dualWriteCallsAddWithFullDimensions() {
        writer.dualWrite(10L, 20L, 30L, 40L, 50L, "BATCH-001", new BigDecimal("100"), null);

        ArgumentCaptor<Long> itemId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> warehouseId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> locationId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> areaId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> batchId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> batchCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> delta = ArgumentCaptor.forClass(BigDecimal.class);

        verify(inventoryService).add(
                itemId.capture(), warehouseId.capture(),
                locationId.capture(), areaId.capture(), batchId.capture(),
                batchCode.capture(), delta.capture());

        assertEquals(10L, itemId.getValue());
        assertEquals(20L, warehouseId.getValue());
        assertEquals(30L, locationId.getValue());
        assertEquals(40L, areaId.getValue());
        assertEquals(50L, batchId.getValue());
        assertEquals("BATCH-001", batchCode.getValue());
        assertEquals(new BigDecimal("100"), delta.getValue());
    }

    @Test
    void dualWritePropagatesExceptionAsRuntimeException() {
        when(inventoryService.add(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB 异常"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> writer.dualWrite(1L, 2L, 3L, 4L, 5L, "B1", new BigDecimal("1"), null));

        assertEquals("[MesInventoryDualWriter] 双写 inventory_item 失败，触发事务回滚", ex.getMessage());
    }

    @Test
    void dualWriteDoesNotAcceptLockedDelta() {
        // MES 无锁定原语，lockedDelta 应被忽略（不传入 inventoryService）
        writer.dualWrite(1L, 2L, 3L, 4L, 5L, "B1", new BigDecimal("5"), new BigDecimal("2"));

        ArgumentCaptor<BigDecimal> delta = ArgumentCaptor.forClass(BigDecimal.class);
        verify(inventoryService).add(any(), any(), any(), any(), any(), any(), delta.capture());
        // quantityDelta 传入，lockedDelta 不单独处理
        assertEquals(new BigDecimal("5"), delta.getValue());
        verify(inventoryService, never()).reserve(any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).release(any(), any(), any(), any(), any(), any());
    }
}
