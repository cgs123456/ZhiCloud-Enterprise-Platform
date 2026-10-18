package cn.zhicloud.module.wms.service.order.pickpath;

import cn.zhicloud.module.wms.controller.admin.pickpath.vo.WmsPickPathRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.dal.mysql.order.wave.WmsWaveOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.wave.WmsWaveOrderMapper;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V211（ADR 269 D6）：拣货路径页 buildLocation seed 真值化单测——
 * 真库位优先作 seed；未定位行回退库存行 id（伪坐标基线口径不变）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsPickPathServiceImplTest {

    @InjectMocks
    private WmsPickPathServiceImpl pickPathService;

    @Mock
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Mock
    private WmsShipmentOrderDetailMapper shipmentOrderDetailMapper;
    @Mock
    private WmsWaveOrderMapper waveOrderMapper;
    @Mock
    private WmsWaveOrderDetailMapper waveOrderDetailMapper;
    @Mock
    private WmsInventoryMapper inventoryMapper;
    @Mock
    private WmsWarehouseService warehouseService;
    @Mock
    private WmsItemSkuService itemSkuService;
    @Mock
    private WmsItemService itemService;
    @Mock
    private WmsLocationService locationService;

    @Test
    public void testOptimizePickPath_locationSeededCoordinates() {
        mockOrderAndDetail();
        // 真库位 200 的行（库存行 id 999 仅作区分：若仍按 id 取 seed 则为 (99, 9)）
        WmsInventoryDO locatedRow = WmsInventoryDO.builder().id(999L).skuId(500L).warehouseId(100L)
                .locationId(200L).quantity(new BigDecimal("5.00"))
                .availableQuantity(new BigDecimal("5.00")).build();
        when(inventoryMapper.selectListBySkuIdAndWarehouseId(500L, 100L)).thenReturn(List.of(locatedRow));
        when(locationService.selectFrozenLocationIds(any())).thenReturn(Collections.emptySet());
        when(itemSkuService.getItemSkuMap(any())).thenReturn(new HashMap<>());

        WmsPickPathRespVO respVO = pickPathService.optimizePickPath(1L);

        // 真库位 200 作 seed：coord = (200 % 100, (200 / 100) % 100) = (0, 2)
        assertEquals(1, respVO.getLocationCount().intValue());
        assertEquals(0, respVO.getLocations().get(0).getCoordX().intValue());
        assertEquals(2, respVO.getLocations().get(0).getCoordY().intValue());
        assertEquals(999L, respVO.getLocations().get(0).getInventoryId());
        // 预估路径长度：(0,0)→(0,2) 去 2 + 回程 2 = 4.00
        assertEquals(0, new BigDecimal("4.00").compareTo(respVO.getEstimatedDistance()));
    }

    @Test
    public void testOptimizePickPath_unlocatedFallsBackToInventoryId() {
        mockOrderAndDetail();
        // 未定位行（locationId 0）→ 回退库存行 id 999 作 seed：(99, 9)
        WmsInventoryDO zeroRow = WmsInventoryDO.builder().id(999L).skuId(500L).warehouseId(100L)
                .quantity(new BigDecimal("5.00")).availableQuantity(new BigDecimal("5.00")).build();
        when(inventoryMapper.selectListBySkuIdAndWarehouseId(500L, 100L)).thenReturn(List.of(zeroRow));
        when(locationService.selectFrozenLocationIds(any())).thenReturn(Collections.emptySet());
        when(itemSkuService.getItemSkuMap(any())).thenReturn(new HashMap<>());

        WmsPickPathRespVO respVO = pickPathService.optimizePickPath(1L);

        assertEquals(99, respVO.getLocations().get(0).getCoordX().intValue());
        assertEquals(9, respVO.getLocations().get(0).getCoordY().intValue());
        // 去 99+9 + 回 99+9 = 216.00
        assertEquals(0, new BigDecimal("216.00").compareTo(respVO.getEstimatedDistance()));
    }

    private void mockOrderAndDetail() {
        when(shipmentOrderMapper.selectById(1L)).thenReturn(WmsShipmentOrderDO.builder()
                .id(1L).no("CK-PATH-001").warehouseId(100L).build());
        when(shipmentOrderDetailMapper.selectListByOrderId(1L)).thenReturn(List.of(
                WmsShipmentOrderDetailDO.builder().skuId(500L).warehouseId(100L)
                        .quantity(new BigDecimal("2.00")).build()));
    }

}
