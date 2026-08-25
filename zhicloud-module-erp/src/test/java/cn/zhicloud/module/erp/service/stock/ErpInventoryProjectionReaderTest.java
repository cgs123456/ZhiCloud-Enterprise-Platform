package cn.zhicloud.module.erp.service.stock;

import cn.zhicloud.framework.inventory.service.InventoryProjection;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ErpInventoryProjectionReader 映射逻辑单元测试（M2 只读投影适配）
 *
 * <p>ERP 无 DB 测试基座、缺 create_tables.sql，故以 mock mapper 验证投影映射正确性，
 * 不依赖 Spring 上下文与 H2 schema。真实端到端对账集成测试随 Task #94（ERP 写路径改写）一并建立。
 *
 * @author 智云库存治理
 */
@ExtendWith(MockitoExtension.class)
public class ErpInventoryProjectionReaderTest {

    @Mock
    private ErpStockMapper stockMapper;
    private ErpInventoryProjectionReader reader;

    @BeforeEach
    void init() {
        reader = new ErpInventoryProjectionReader(stockMapper);
    }

    @Test
    void readAllMapsProductIdToItemAndNullsMissingDimensions() {
        ErpStockDO s1 = new ErpStockDO();
        s1.setProductId(11L);
        s1.setWarehouseId(22L);
        s1.setCount(new BigDecimal("7"));
        s1.setLockedCount(new BigDecimal("2"));

        ErpStockDO s2 = new ErpStockDO();
        s2.setProductId(33L);
        s2.setWarehouseId(44L);
        s2.setCount(BigDecimal.ZERO);
        s2.setLockedCount(null);

        when(stockMapper.selectList(any())).thenReturn(List.of(s1, s2));

        List<InventoryProjection> projections = reader.readAll();

        assertEquals(2, projections.size());

        InventoryProjection p1 = projections.get(0);
        assertEquals("erp", p1.getSource());
        assertEquals(11L, p1.getItemId());
        assertEquals(22L, p1.getWarehouseId());
        assertNull(p1.getLocationId(), "ERP 无库位维度，应置 null 以与真值源对齐");
        assertNull(p1.getAreaId());
        assertNull(p1.getBatchId());
        assertEquals(0, new BigDecimal("7").compareTo(p1.getQuantity()));
        assertEquals(0, new BigDecimal("2").compareTo(p1.getLockedCount()));

        InventoryProjection p2 = projections.get(1);
        assertEquals(0, BigDecimal.ZERO.compareTo(p2.getQuantity()));
        assertNull(p2.getLockedCount());
    }

}
