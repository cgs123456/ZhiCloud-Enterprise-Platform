package cn.iocoder.yudao.module.wms.service.md.location;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.iocoder.yudao.module.wms.service.md.zone.WmsZoneService;
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
        reqVO.setStatus(0);
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

        locationService.deleteLocation(5L);

        verify(locationMapper).deleteById(5L);
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

}
