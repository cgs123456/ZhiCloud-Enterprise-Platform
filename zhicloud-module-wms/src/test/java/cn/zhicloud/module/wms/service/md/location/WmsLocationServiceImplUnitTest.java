package cn.zhicloud.module.wms.service.md.location;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * {@link WmsLocationServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsLocationServiceImplUnitTest {

    @Mock
    private WmsLocationMapper locationMapper;
    @Mock
    private WmsZoneService zoneService;
    @Mock
    private WmsInventoryMapper inventoryMapper; // V205（ADR 259 D9）：deleteLocation 存量校验/冲洗
    @Mock
    private WmsInventoryBatchMapper inventoryBatchMapper; // V211（ADR 269 D9）：deleteLocation 批次随行物理冲洗

    @InjectMocks
    private WmsLocationServiceImpl locationService;

    private static WmsLocationSaveReqVO buildSaveReqVO(Long id, String barcode) {
        WmsLocationSaveReqVO reqVO = new WmsLocationSaveReqVO();
        reqVO.setId(id);
        reqVO.setZoneId(10L);
        reqVO.setCode("L001");
        reqVO.setName("A-01-01");
        reqVO.setBarcode(barcode);
        reqVO.setType(1);
        reqVO.setStatus(10);
        reqVO.setSort(1);
        return reqVO;
    }

    @Test
    public void testCreateLocation_success_andWarehouseIdBackfilled() {
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(null);
        when(locationMapper.selectByBarcode("BC001")).thenReturn(null);
        doAnswer(invocation -> {
            WmsLocationDO arg = invocation.getArgument(0);
            arg.setId(200L);
            return 1;
        }).when(locationMapper).insert(any(WmsLocationDO.class));

        WmsLocationSaveReqVO reqVO = buildSaveReqVO(null, "BC001");
        Long id = locationService.createLocation(reqVO);

        assertEquals(200L, id);
        // 仓库 ID 以库区所属仓库为准回填
        assertEquals(77L, reqVO.getWarehouseId());
    }

    @Test
    public void testCreateLocation_barcodeBlankSkipsCheck() {
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(null);

        locationService.createLocation(buildSaveReqVO(null, ""));

        verify(locationMapper, never()).selectByBarcode(any());
    }

    @Test
    public void testCreateLocation_codeDuplicate() {
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(WmsLocationDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.createLocation(buildSaveReqVO(null, "BC001")));
        assertEquals(LOCATION_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateLocation_barcodeDuplicate() {
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(null);
        when(locationMapper.selectByBarcode("BC001")).thenReturn(WmsLocationDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.createLocation(buildSaveReqVO(null, "BC001")));
        assertEquals(LOCATION_BARCODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateLocation_statusForcedToIdle() {
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(null);
        when(locationMapper.selectByBarcode("BC001")).thenReturn(null);
        doAnswer(invocation -> {
            WmsLocationDO arg = invocation.getArgument(0);
            arg.setId(200L);
            return 1;
        }).when(locationMapper).insert(any(WmsLocationDO.class));

        // 调用方试图建单即锁定：status 应被服务端强制为「空闲」，忽略外部输入
        WmsLocationSaveReqVO reqVO = buildSaveReqVO(null, "BC001");
        reqVO.setStatus(WmsLocationStatusEnum.LOCKED.getStatus());
        Long id = locationService.createLocation(reqVO);

        assertEquals(200L, id);
        ArgumentCaptor<WmsLocationDO> captor = ArgumentCaptor.forClass(WmsLocationDO.class);
        verify(locationMapper).insert(captor.capture());
        assertEquals(WmsLocationStatusEnum.IDLE.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testUpdateLocation_success() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L).build());
        when(zoneService.validateZoneExists(10L)).thenReturn(WmsZoneDO.builder().id(10L).warehouseId(77L).build());
        when(locationMapper.selectByCode(10L, "L001")).thenReturn(WmsLocationDO.builder().id(5L).build());
        when(locationMapper.selectByBarcode("BC001")).thenReturn(WmsLocationDO.builder().id(5L).build());

        locationService.updateLocation(buildSaveReqVO(5L, "BC001"));

        verify(locationMapper).updateById(any(WmsLocationDO.class));
    }

    @Test
    public void testUpdateLocation_notExists() {
        when(locationMapper.selectById(5L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.updateLocation(buildSaveReqVO(5L, "BC001")));
        assertEquals(LOCATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteLocation_success() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L).build());
        // V205：deleteLocation 先校验库位下库存行（无行空集 → 直接放行逻辑删）
        when(inventoryMapper.selectListByLocationId(5L)).thenReturn(Collections.emptyList());

        locationService.deleteLocation(5L);

        verify(locationMapper).deleteById(5L);
    }

    @Test
    public void testDeleteLocation_zeroRowsWashedWithBatches() {
        when(locationMapper.selectById(7L)).thenReturn(WmsLocationDO.builder().id(7L).build());
        // V211（ADR 269 D9）：纯零库存行连同其批次行同批物理冲洗（uk(inventory_id, batch_no) 不含 deleted）
        when(inventoryMapper.selectListByLocationId(7L)).thenReturn(List.of(
                WmsInventoryDO.builder().id(31L).quantity(BigDecimal.ZERO)
                        .availableQuantity(BigDecimal.ZERO).build(),
                WmsInventoryDO.builder().id(32L).quantity(BigDecimal.ZERO)
                        .availableQuantity(BigDecimal.ZERO).build()));

        locationService.deleteLocation(7L);

        verify(inventoryMapper).deleteByIdListPhysical(List.of(31L, 32L));
        verify(inventoryBatchMapper).deleteByInventoryIdListPhysical(List.of(31L, 32L));
        verify(locationMapper).deleteById(7L);
    }

    @Test
    public void testValidateLocationExists_notExists() {
        when(locationMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.validateLocationExists(404L));
        assertEquals(LOCATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testQueryMethods() {
        when(locationMapper.selectById(1L)).thenReturn(WmsLocationDO.builder().id(1L).build());
        when(locationMapper.selectPage(any(WmsLocationPageReqVO.class))).thenReturn(PageResult.empty());
        when(locationMapper.selectList()).thenReturn(List.of(WmsLocationDO.builder().id(1L).build()));
        when(locationMapper.selectListByZoneId(10L)).thenReturn(List.of(WmsLocationDO.builder().id(1L).build()));
        when(locationMapper.selectListByWarehouseId(77L)).thenReturn(List.of(WmsLocationDO.builder().id(1L).build()));
        when(locationMapper.selectByIds(anyCollection())).thenReturn(List.of(WmsLocationDO.builder().id(1L).build()));

        assertNotNull(locationService.getLocation(1L));
        assertNotNull(locationService.getLocationPage(new WmsLocationPageReqVO()));
        assertEquals(1, locationService.getLocationList().size());
        assertEquals(1, locationService.getLocationListByZoneId(10L).size());
        assertEquals(1, locationService.getLocationListByWarehouseId(77L).size());
        assertEquals(1, locationService.getLocationList(List.of(1L)).size());
        assertTrue(locationService.getLocationList(Collections.emptyList()).isEmpty());
    }

    // ==================== V205（§12.259，ADR 259 D3/D4）：冻结集批查与占用派生 ====================

    @Test
    public void testSelectFrozenLocationIds_onlyLockedAndDisabled() {
        when(locationMapper.selectByIds(anyCollection())).thenReturn(Arrays.asList(
                WmsLocationDO.builder().id(1L).status(WmsLocationStatusEnum.IDLE.getStatus()).build(),
                WmsLocationDO.builder().id(2L).status(WmsLocationStatusEnum.LOCKED.getStatus()).build(),
                WmsLocationDO.builder().id(3L).status(WmsLocationStatusEnum.DISABLED.getStatus()).build(),
                WmsLocationDO.builder().id(4L).status(WmsLocationStatusEnum.OCCUPIED.getStatus()).build()));

        Set<Long> frozen = locationService.selectFrozenLocationIds(Arrays.asList(1L, 2L, 3L, 4L));

        assertEquals(new HashSet<>(Arrays.asList(2L, 3L)), frozen);
    }

    @Test
    public void testRefreshOccupancyStatus_managesStatesSkippedAndDerives() {
        // LOCKED 管理态：人工裁决，不派生（0/null 哨兵同样跳过）
        when(locationMapper.selectById(11L)).thenReturn(WmsLocationDO.builder().id(11L)
                .status(WmsLocationStatusEnum.LOCKED.getStatus()).build());
        when(locationMapper.selectById(12L)).thenReturn(WmsLocationDO.builder().id(12L)
                .status(WmsLocationStatusEnum.IDLE.getStatus()).build());
        when(inventoryMapper.selectCountByLocationIdAndQuantityPositive(12L)).thenReturn(2L);

        locationService.refreshOccupancyStatus(Arrays.asList(null, 0L, 11L, 12L));

        // 仅有货的 12 号库位派生为 OCCUPIED；11 号（LOCKED）与哨兵零写入
        ArgumentCaptor<WmsLocationDO> captor = ArgumentCaptor.forClass(WmsLocationDO.class);
        verify(locationMapper).updateById(captor.capture());
        assertEquals(12L, captor.getValue().getId());
        assertEquals(WmsLocationStatusEnum.OCCUPIED.getStatus(), captor.getValue().getStatus());
    }

    // ==================== 断点 4 新增：状态值域与可用性门禁 ====================

    @Test
    public void testCreateLocation_statusInvalid() {
        WmsLocationSaveReqVO reqVO = buildSaveReqVO(null, "BC001");
        reqVO.setStatus(99);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.createLocation(reqVO));
        assertEquals(LOCATION_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateLocation_statusInvalid() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L).build());
        WmsLocationSaveReqVO reqVO = buildSaveReqVO(5L, "BC001");
        reqVO.setStatus(0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.updateLocation(reqVO));
        assertEquals(LOCATION_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testValidateLocationUsable_notExists() {
        when(locationMapper.selectById(404L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.validateLocationUsable(404L));
        assertEquals(LOCATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testValidateLocationUsable_locked() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L)
                .status(WmsLocationStatusEnum.LOCKED.getStatus()).build());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.validateLocationUsable(5L));
        assertEquals(LOCATION_LOCKED.getCode(), ex.getCode());
    }

    @Test
    public void testValidateLocationUsable_disabled() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L)
                .status(WmsLocationStatusEnum.DISABLED.getStatus()).build());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> locationService.validateLocationUsable(5L));
        assertEquals(LOCATION_DISABLED.getCode(), ex.getCode());
    }

    @Test
    public void testValidateLocationUsable_idleAndOccupiedPass() {
        when(locationMapper.selectById(5L)).thenReturn(WmsLocationDO.builder().id(5L)
                .status(WmsLocationStatusEnum.IDLE.getStatus()).build());
        assertEquals(5L, locationService.validateLocationUsable(5L).getId());
        when(locationMapper.selectById(6L)).thenReturn(WmsLocationDO.builder().id(6L)
                .status(WmsLocationStatusEnum.OCCUPIED.getStatus()).build());
        assertEquals(6L, locationService.validateLocationUsable(6L).getId());
    }

}
