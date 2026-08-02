package cn.iocoder.yudao.module.wms.service.md.zone;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.md.zone.vo.WmsZonePageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.zone.vo.WmsZoneSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.zone.WmsZoneMapper;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsZoneServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsZoneServiceImplUnitTest {

    @Mock
    private WmsZoneMapper zoneMapper;
    @Mock
    private WmsLocationMapper locationMapper;
    @Mock
    private WmsWarehouseService warehouseService;

    @InjectMocks
    private WmsZoneServiceImpl zoneService;

    private static WmsZoneSaveReqVO buildSaveReqVO(Long id) {
        WmsZoneSaveReqVO reqVO = new WmsZoneSaveReqVO();
        reqVO.setId(id);
        reqVO.setWarehouseId(1L);
        reqVO.setCode("Z001");
        reqVO.setName("拣货区");
        reqVO.setType(1);
        reqVO.setSort(1);
        return reqVO;
    }

    @Test
    public void testCreateZone_success() {
        when(warehouseService.validateWarehouseExists(1L)).thenReturn(new WmsWarehouseDO());
        when(zoneMapper.selectByCode(1L, "Z001")).thenReturn(null);
        when(zoneMapper.selectByName(1L, "拣货区")).thenReturn(null);
        doAnswer(invocation -> {
            WmsZoneDO arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        }).when(zoneMapper).insert(any(WmsZoneDO.class));

        Long id = zoneService.createZone(buildSaveReqVO(null));

        assertEquals(100L, id);
        verify(zoneMapper).insert(any(WmsZoneDO.class));
    }

    @Test
    public void testCreateZone_codeDuplicate() {
        when(warehouseService.validateWarehouseExists(1L)).thenReturn(new WmsWarehouseDO());
        when(zoneMapper.selectByCode(1L, "Z001")).thenReturn(WmsZoneDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> zoneService.createZone(buildSaveReqVO(null)));
        assertEquals(ZONE_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateZone_nameDuplicate() {
        when(warehouseService.validateWarehouseExists(1L)).thenReturn(new WmsWarehouseDO());
        when(zoneMapper.selectByCode(1L, "Z001")).thenReturn(null);
        when(zoneMapper.selectByName(1L, "拣货区")).thenReturn(WmsZoneDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> zoneService.createZone(buildSaveReqVO(null)));
        assertEquals(ZONE_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateZone_success() {
        when(zoneMapper.selectById(5L)).thenReturn(WmsZoneDO.builder().id(5L).build());
        when(warehouseService.validateWarehouseExists(1L)).thenReturn(new WmsWarehouseDO());
        // 命中自身，不算重复
        when(zoneMapper.selectByCode(1L, "Z001")).thenReturn(WmsZoneDO.builder().id(5L).build());
        when(zoneMapper.selectByName(1L, "拣货区")).thenReturn(WmsZoneDO.builder().id(5L).build());

        zoneService.updateZone(buildSaveReqVO(5L));

        verify(zoneMapper).updateById(any(WmsZoneDO.class));
    }

    @Test
    public void testUpdateZone_notExists() {
        when(zoneMapper.selectById(5L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> zoneService.updateZone(buildSaveReqVO(5L)));
        assertEquals(ZONE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteZone_success() {
        when(zoneMapper.selectById(5L)).thenReturn(WmsZoneDO.builder().id(5L).build());
        when(locationMapper.selectCountByZoneId(5L)).thenReturn(0L);

        zoneService.deleteZone(5L);

        verify(zoneMapper).deleteById(5L);
    }

    @Test
    public void testDeleteZone_hasLocations() {
        when(zoneMapper.selectById(5L)).thenReturn(WmsZoneDO.builder().id(5L).build());
        when(locationMapper.selectCountByZoneId(5L)).thenReturn(3L);

        ServiceException ex = assertThrows(ServiceException.class, () -> zoneService.deleteZone(5L));
        assertEquals(ZONE_HAS_LOCATIONS.getCode(), ex.getCode());
        verify(zoneMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testValidateZoneExists_notExists() {
        when(zoneMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> zoneService.validateZoneExists(404L));
        assertEquals(ZONE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testQueryMethods() {
        when(zoneMapper.selectById(1L)).thenReturn(WmsZoneDO.builder().id(1L).build());
        when(zoneMapper.selectPage(any(WmsZonePageReqVO.class))).thenReturn(PageResult.empty());
        when(zoneMapper.selectList()).thenReturn(List.of(WmsZoneDO.builder().id(1L).build()));
        when(zoneMapper.selectListByWarehouseId(1L)).thenReturn(List.of(WmsZoneDO.builder().id(1L).build()));
        when(zoneMapper.selectByIds(anyCollection())).thenReturn(List.of(WmsZoneDO.builder().id(1L).build()));

        assertNotNull(zoneService.getZone(1L));
        assertNotNull(zoneService.getZonePage(new WmsZonePageReqVO()));
        assertEquals(1, zoneService.getZoneList().size());
        assertEquals(1, zoneService.getZoneListByWarehouseId(1L).size());
        assertEquals(1, zoneService.getZoneList(List.of(1L)).size());
        // 空集合直接短路
        assertTrue(zoneService.getZoneList(Collections.emptyList()).isEmpty());
    }

}
