package cn.iocoder.yudao.module.mes.service.wm.warehouse;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.warehouse.vo.area.MesWmWarehouseAreaPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.warehouse.vo.area.MesWmWarehouseAreaSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.warehouse.MesWmWarehouseAreaMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmWarehouseAreaServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmWarehouseAreaServiceImpl.class)
public class MesWmWarehouseAreaServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmWarehouseAreaMapper areaMapper;
    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private MesWmMaterialStockService materialStockService;
    @MockitoBean
    private MesWmWarehouseLocationService locationService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;

    @Resource
    private MesWmWarehouseAreaServiceImpl areaService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmWarehouseAreaDO area = inv.getArgument(0);
            area.setId(area.getId() == null ? 100L : area.getId());
            return 1;
        }).when(areaMapper).insert(any(MesWmWarehouseAreaDO.class));
        when(areaMapper.updateById(any(MesWmWarehouseAreaDO.class))).thenReturn(1);
        when(areaMapper.deleteById(anyLong())).thenReturn(1);
        // 引用计数默认 0
        when(workstationService.getWorkstationCountByAreaId(anyLong())).thenReturn(0L);
        when(materialStockService.getMaterialStockCountByAreaId(anyLong())).thenReturn(0L);
    }

    private MesWmWarehouseAreaDO buildArea() {
        return new MesWmWarehouseAreaDO().setId(100L).setCode("A001").setName("默认库位")
                .setLocationId(10L).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setFrozen(false).setAllowItemMixing(true).setAllowBatchMixing(true);
    }

    private MesWmWarehouseAreaSaveReqVO buildSaveReq() {
        MesWmWarehouseAreaSaveReqVO reqVO = new MesWmWarehouseAreaSaveReqVO();
        reqVO.setCode("A001");
        reqVO.setName("默认库位");
        reqVO.setLocationId(10L);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setFrozen(false);
        reqVO.setAllowItemMixing(true);
        reqVO.setAllowBatchMixing(true);
        return reqVO;
    }

    private MesWmWarehouseLocationDO buildLocation() {
        return new MesWmWarehouseLocationDO().setId(10L).setCode("L001").setName("默认库区")
                .setWarehouseId(5L);
    }

    // ========== createWarehouseArea ==========

    @Test
    public void testCreateWarehouseArea_success() {
        Long id = areaService.createWarehouseArea(buildSaveReq());
        assertEquals(100L, id);
        verify(areaMapper).insert(any(MesWmWarehouseAreaDO.class));
        verify(locationService).validateWarehouseLocationExists(10L);
        verify(barcodeService).autoGenerateBarcode(any(), eq(100L), eq("A001"), eq("默认库位"));
    }

    @Test
    public void testCreateWarehouseArea_isVirtual() {
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);
        assertServiceException(() -> areaService.createWarehouseArea(reqVO), WM_WAREHOUSE_AREA_IS_VIRTUAL);
        verify(areaMapper, never()).insert(any(MesWmWarehouseAreaDO.class));
    }

    @Test
    public void testCreateWarehouseArea_codeDuplicate() {
        when(areaMapper.selectByCode(10L, "A001")).thenReturn(buildArea());
        assertServiceException(() -> areaService.createWarehouseArea(buildSaveReq()), WM_WAREHOUSE_AREA_CODE_DUPLICATE);
    }

    @Test
    public void testCreateWarehouseArea_nameDuplicate() {
        when(areaMapper.selectByName(10L, "默认库位")).thenReturn(buildArea());
        assertServiceException(() -> areaService.createWarehouseArea(buildSaveReq()), WM_WAREHOUSE_AREA_NAME_DUPLICATE);
    }

    // ========== updateWarehouseArea ==========

    @Test
    public void testUpdateWarehouseArea_success() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setName("库位B");
        areaService.updateWarehouseArea(reqVO);
        verify(areaMapper).updateById(any(MesWmWarehouseAreaDO.class));
    }

    @Test
    public void testUpdateWarehouseArea_notExists() {
        when(areaMapper.selectById(100L)).thenReturn(null);
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> areaService.updateWarehouseArea(reqVO), WM_WAREHOUSE_AREA_NOT_EXISTS);
    }

    @Test
    public void testUpdateWarehouseArea_isVirtual() {
        when(areaMapper.selectById(100L)).thenReturn(
                buildArea().setCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA));
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> areaService.updateWarehouseArea(reqVO), WM_WAREHOUSE_AREA_IS_VIRTUAL);
    }

    @Test
    public void testUpdateWarehouseArea_codeDuplicate() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(areaMapper.selectByCode(10L, "A002")).thenReturn(buildArea().setId(200L).setCode("A002"));
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setCode("A002");
        assertServiceException(() -> areaService.updateWarehouseArea(reqVO), WM_WAREHOUSE_AREA_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateWarehouseArea_nameDuplicate() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(areaMapper.selectByName(10L, "库位B")).thenReturn(buildArea().setId(200L).setName("库位B"));
        MesWmWarehouseAreaSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setName("库位B");
        assertServiceException(() -> areaService.updateWarehouseArea(reqVO), WM_WAREHOUSE_AREA_NAME_DUPLICATE);
    }

    // ========== deleteWarehouseArea ==========

    @Test
    public void testDeleteWarehouseArea_success() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        areaService.deleteWarehouseArea(100L);
        verify(areaMapper).deleteById(100L);
    }

    @Test
    public void testDeleteWarehouseArea_notExists() {
        when(areaMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> areaService.deleteWarehouseArea(100L), WM_WAREHOUSE_AREA_NOT_EXISTS);
    }

    @Test
    public void testDeleteWarehouseArea_isVirtual() {
        when(areaMapper.selectById(100L)).thenReturn(
                buildArea().setCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA));
        assertServiceException(() -> areaService.deleteWarehouseArea(100L), WM_WAREHOUSE_AREA_IS_VIRTUAL);
    }

    @Test
    public void testDeleteWarehouseArea_hasWorkstation() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(workstationService.getWorkstationCountByAreaId(100L)).thenReturn(1L);
        assertServiceException(() -> areaService.deleteWarehouseArea(100L), WM_WAREHOUSE_AREA_HAS_WORKSTATION);
    }

    @Test
    public void testDeleteWarehouseArea_hasMaterialStock() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(materialStockService.getMaterialStockCountByAreaId(100L)).thenReturn(2L);
        assertServiceException(() -> areaService.deleteWarehouseArea(100L), WM_WAREHOUSE_AREA_HAS_MATERIAL_STOCK);
    }

    // ========== validateWarehouseAreaExists ==========

    @Test
    public void testValidateWarehouseAreaExists_success() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        assertNotNull(areaService.validateWarehouseAreaExists(100L));
    }

    @Test
    public void testValidateWarehouseAreaExists_notExists() {
        when(areaMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> areaService.validateWarehouseAreaExists(100L), WM_WAREHOUSE_AREA_NOT_EXISTS);
    }

    @Test
    public void testValidateWarehouseAreaExistsWithRelation_areaIdNull() {
        areaService.validateWarehouseAreaExists(5L, 10L, null);
        verify(areaMapper, never()).selectById(anyLong());
    }

    @Test
    public void testValidateWarehouseAreaExistsWithRelation_success() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(locationService.validateWarehouseLocationExists(10L)).thenReturn(buildLocation());
        areaService.validateWarehouseAreaExists(5L, 10L, 100L);
        verify(locationService, atLeastOnce()).validateWarehouseLocationExists(10L);
    }

    @Test
    public void testValidateWarehouseAreaExistsWithRelation_relationInvalid() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        assertServiceException(() -> areaService.validateWarehouseAreaExists(5L, 20L, 100L),
                WM_WAREHOUSE_AREA_RELATION_INVALID);
    }

    @Test
    public void testValidateWarehouseAreaExistsWithRelation_warehouseMismatch() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        when(locationService.validateWarehouseLocationExists(10L)).thenReturn(buildLocation().setWarehouseId(9L));
        assertServiceException(() -> areaService.validateWarehouseAreaExists(5L, 10L, 100L),
                WM_WAREHOUSE_AREA_WAREHOUSE_MISMATCH);
    }

    // ========== get / list / page ==========

    @Test
    public void testGetWarehouseArea() {
        when(areaMapper.selectById(100L)).thenReturn(buildArea());
        assertNotNull(areaService.getWarehouseArea(100L));
    }

    @Test
    public void testGetWarehouseAreaPage() {
        PageResult<MesWmWarehouseAreaDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(areaMapper.selectPage(any(MesWmWarehouseAreaPageReqVO.class))).thenReturn(page);
        assertEquals(0, areaService.getWarehouseAreaPage(new MesWmWarehouseAreaPageReqVO()).getTotal());
    }

    @Test
    public void testGetWarehouseAreaListByLocationId() {
        when(areaMapper.selectSimpleList(10L)).thenReturn(Arrays.asList(buildArea()));
        assertEquals(1, areaService.getWarehouseAreaList(10L).size());
    }

    @Test
    public void testGetWarehouseAreaListByIds_empty() {
        assertTrue(areaService.getWarehouseAreaList(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testGetWarehouseAreaListByIds_nonEmpty() {
        when(areaMapper.selectListByIds(any())).thenReturn(Arrays.asList(buildArea()));
        assertEquals(1, areaService.getWarehouseAreaList(Arrays.asList(100L)).size());
    }

    @Test
    public void testGetWarehouseAreaCountByLocationId() {
        when(areaMapper.selectCountByLocationId(10L)).thenReturn(3L);
        assertEquals(3L, areaService.getWarehouseAreaCountByLocationId(10L));
    }

    // ========== updateByLocationId ==========

    @Test
    public void testUpdateByLocationId() {
        areaService.updateByLocationId(10L, true, false);
        verify(locationService).validateWarehouseLocationExists(10L);
        verify(areaMapper).updateByLocationId(eq(10L), any(MesWmWarehouseAreaDO.class));
    }

    @Test
    public void testUpdateByLocationId_allNull() {
        areaService.updateByLocationId(10L, null, null);
        verify(areaMapper).updateByLocationId(eq(10L), any(MesWmWarehouseAreaDO.class));
    }

    // ========== getWarehouseAreaByCode ==========

    @Test
    public void testGetWarehouseAreaByCode_exists() {
        when(areaMapper.selectByCode("A001")).thenReturn(buildArea());
        assertNotNull(areaService.getWarehouseAreaByCode("A001"));
        verify(areaMapper, never()).insert(any(MesWmWarehouseAreaDO.class));
    }

    @Test
    public void testGetWarehouseAreaByCode_notExists() {
        when(areaMapper.selectByCode("A999")).thenReturn(null);
        assertNull(areaService.getWarehouseAreaByCode("A999"));
    }

    @Test
    public void testGetWarehouseAreaByCode_virtualAutoInit() {
        when(areaMapper.selectByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA)).thenReturn(null);
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(buildLocation());
        MesWmWarehouseAreaDO area = areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);
        assertNotNull(area);
        assertEquals(10L, area.getLocationId());
        assertEquals("虚拟线边库位", area.getName());
        verify(areaMapper).insert(any(MesWmWarehouseAreaDO.class));
    }

}
