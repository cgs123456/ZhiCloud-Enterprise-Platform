package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.service.dv.checkplan.MesDvCheckPlanMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.MesDvCheckRecordService;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.MesDvMaintenRecordService;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesDvMachineryServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesDvMachineryServiceImpl.class)
public class MesDvMachineryServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesDvMachineryMapper machineryMapper;
    @MockitoBean
    private MesDvMachineryTypeService machineryTypeService;
    @MockitoBean
    private MesMdWorkshopService workshopService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesDvCheckPlanMachineryService checkPlanMachineryService;
    @MockitoBean
    private MesDvCheckRecordService checkRecordService;
    @MockitoBean
    private MesDvMaintenRecordService maintenRecordService;
    @MockitoBean
    private MesDvRepairService repairService;

    @Resource
    private MesDvMachineryServiceImpl machineryService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesDvMachineryDO machinery = inv.getArgument(0);
            machinery.setId(machinery.getId() == null ? 100L : machinery.getId());
            return 1;
        }).when(machineryMapper).insert(any(MesDvMachineryDO.class));
        when(machineryMapper.updateById(any(MesDvMachineryDO.class))).thenReturn(1);
        when(machineryMapper.deleteById(anyLong())).thenReturn(1);
        // 引用计数默认 0
        when(checkPlanMachineryService.getCheckPlanMachineryCountByMachineryId(anyLong())).thenReturn(0L);
        when(checkRecordService.getCheckRecordCountByMachineryId(anyLong())).thenReturn(0L);
        when(maintenRecordService.getMaintenRecordCountByMachineryId(anyLong())).thenReturn(0L);
        when(repairService.getRepairCountByMachineryId(anyLong())).thenReturn(0L);
    }

    private MesDvMachineryDO buildMachinery() {
        return new MesDvMachineryDO().setId(100L).setCode("EQ-001").setName("CNC 加工中心")
                .setBrand("西门子").setSpecification("S7-300")
                .setMachineryTypeId(1L).setWorkshopId(2L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesDvMachinerySaveReqVO buildSaveReq() {
        return new MesDvMachinerySaveReqVO().setCode("EQ-001").setName("CNC 加工中心")
                .setMachineryTypeId(1L).setWorkshopId(2L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private void mockImportBaseData() {
        MesDvMachineryTypeDO type = new MesDvMachineryTypeDO().setId(1L).setCode("T001").setName("数控机床");
        when(machineryTypeService.getMachineryTypeList(any(MesDvMachineryTypeListReqVO.class)))
                .thenReturn(Arrays.asList(type));
        MesMdWorkshopDO workshop = new MesMdWorkshopDO().setId(2L).setCode("W001").setName("一号车间");
        when(workshopService.getWorkshopListByStatus(anyInt())).thenReturn(Arrays.asList(workshop));
    }

    private MesDvMachineryImportExcelVO buildImportVO() {
        return new MesDvMachineryImportExcelVO().setCode("EQ-001").setName("CNC 加工中心")
                .setMachineryTypeCode("T001").setWorkshopCode("W001")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    // ========== createMachinery ==========

    @Test
    public void testCreateMachinery_success() {
        Long id = machineryService.createMachinery(buildSaveReq());
        assertEquals(100L, id);
        verify(machineryMapper).insert(any(MesDvMachineryDO.class));
        verify(machineryTypeService).getMachineryType(1L);
        verify(workshopService).getWorkshop(2L);
        verify(barcodeService).autoGenerateBarcode(any(), eq(100L), eq("EQ-001"), eq("CNC 加工中心"));
    }

    @Test
    public void testCreateMachinery_codeNull() {
        MesDvMachinerySaveReqVO req = buildSaveReq().setCode(null);
        assertEquals(100L, machineryService.createMachinery(req));
        verify(machineryMapper, never()).selectByCode(anyString());
    }

    @Test
    public void testCreateMachinery_codeNotExists() {
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(null);
        assertEquals(100L, machineryService.createMachinery(buildSaveReq()));
    }

    @Test
    public void testCreateMachinery_codeDuplicate() {
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(buildMachinery());
        assertServiceException(() -> machineryService.createMachinery(buildSaveReq()), DV_MACHINERY_CODE_DUPLICATE);
    }

    // ========== updateMachinery ==========

    @Test
    public void testUpdateMachinery_success() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        MesDvMachinerySaveReqVO req = buildSaveReq().setId(100L).setName("CNC 加工中心 V2");
        machineryService.updateMachinery(req);
        verify(machineryMapper).updateById(any(MesDvMachineryDO.class));
    }

    @Test
    public void testUpdateMachinery_sameCodeSelf() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(buildMachinery());
        machineryService.updateMachinery(buildSaveReq().setId(100L));
        verify(machineryMapper).updateById(any(MesDvMachineryDO.class));
    }

    @Test
    public void testUpdateMachinery_notExists() {
        when(machineryMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> machineryService.updateMachinery(buildSaveReq().setId(100L)),
                DV_MACHINERY_NOT_EXISTS);
    }

    @Test
    public void testUpdateMachinery_codeDuplicate() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(buildMachinery().setId(200L));
        assertServiceException(() -> machineryService.updateMachinery(buildSaveReq().setId(100L)),
                DV_MACHINERY_CODE_DUPLICATE);
    }

    // ========== deleteMachinery ==========

    @Test
    public void testDeleteMachinery_success() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        machineryService.deleteMachinery(100L);
        verify(machineryMapper).deleteById(100L);
    }

    @Test
    public void testDeleteMachinery_notExists() {
        when(machineryMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> machineryService.deleteMachinery(100L), DV_MACHINERY_NOT_EXISTS);
    }

    @Test
    public void testDeleteMachinery_hasCheckPlan() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(checkPlanMachineryService.getCheckPlanMachineryCountByMachineryId(100L)).thenReturn(1L);
        assertServiceException(() -> machineryService.deleteMachinery(100L), DV_MACHINERY_HAS_CHECK_PLAN);
    }

    @Test
    public void testDeleteMachinery_hasCheckRecord() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(checkRecordService.getCheckRecordCountByMachineryId(100L)).thenReturn(2L);
        assertServiceException(() -> machineryService.deleteMachinery(100L), DV_MACHINERY_HAS_CHECK_RECORD);
    }

    @Test
    public void testDeleteMachinery_hasMaintenRecord() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(maintenRecordService.getMaintenRecordCountByMachineryId(100L)).thenReturn(3L);
        assertServiceException(() -> machineryService.deleteMachinery(100L), DV_MACHINERY_HAS_MAINTEN_RECORD);
    }

    @Test
    public void testDeleteMachinery_hasRepair() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        when(repairService.getRepairCountByMachineryId(100L)).thenReturn(4L);
        assertServiceException(() -> machineryService.deleteMachinery(100L), DV_MACHINERY_HAS_REPAIR);
    }

    // ========== validateMachineryExists ==========

    @Test
    public void testValidateMachineryExists_success() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        machineryService.validateMachineryExists(100L);
        verify(machineryMapper).selectById(100L);
    }

    @Test
    public void testValidateMachineryExists_notExists() {
        when(machineryMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> machineryService.validateMachineryExists(100L), DV_MACHINERY_NOT_EXISTS);
    }

    // ========== get / list / page ==========

    @Test
    public void testGetMachinery() {
        when(machineryMapper.selectById(100L)).thenReturn(buildMachinery());
        MesDvMachineryDO machinery = machineryService.getMachinery(100L);
        assertNotNull(machinery);
        assertEquals("EQ-001", machinery.getCode());
    }

    @Test
    public void testGetMachinery_null() {
        when(machineryMapper.selectById(999L)).thenReturn(null);
        assertNull(machineryService.getMachinery(999L));
    }

    @Test
    public void testGetMachineryPage_withoutMachineryType() {
        PageResult<MesDvMachineryDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(machineryMapper.selectPage(any(MesDvMachineryPageReqVO.class))).thenReturn(page);
        assertEquals(0, machineryService.getMachineryPage(new MesDvMachineryPageReqVO()).getTotal());
        verify(machineryTypeService, never()).getMachineryTypeChildrenList(anyLong());
    }

    @Test
    public void testGetMachineryPage_withMachineryType() {
        PageResult<MesDvMachineryDO> page = new PageResult<>(Arrays.asList(buildMachinery()), 1L);
        when(machineryMapper.selectPage(any(MesDvMachineryPageReqVO.class))).thenReturn(page);
        when(machineryTypeService.getMachineryTypeChildrenList(1L))
                .thenReturn(Arrays.asList(new MesDvMachineryTypeDO().setId(11L).setCode("T002")));
        MesDvMachineryPageReqVO reqVO = new MesDvMachineryPageReqVO();
        reqVO.setMachineryTypeId(1L);
        assertEquals(1, machineryService.getMachineryPage(reqVO).getTotal());
        assertEquals(2, reqVO.getMachineryTypeIds().size());
    }

    @Test
    public void testGetMachineryCountByMachineryTypeId() {
        when(machineryMapper.selectCountByMachineryTypeId(1L)).thenReturn(5L);
        assertEquals(5L, machineryService.getMachineryCountByMachineryTypeId(1L));
    }

    @Test
    public void testUpdateMachineryLastCheckTime() {
        LocalDateTime now = LocalDateTime.now();
        machineryService.updateMachineryLastCheckTime(100L, now);
        ArgumentCaptor<MesDvMachineryDO> captor = ArgumentCaptor.forClass(MesDvMachineryDO.class);
        verify(machineryMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getId());
        assertEquals(now, captor.getValue().getLastCheckTime());
    }

    @Test
    public void testUpdateMachineryLastMaintenTime() {
        LocalDateTime now = LocalDateTime.now();
        machineryService.updateMachineryLastMaintenTime(100L, now);
        ArgumentCaptor<MesDvMachineryDO> captor = ArgumentCaptor.forClass(MesDvMachineryDO.class);
        verify(machineryMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getId());
        assertEquals(now, captor.getValue().getLastMaintenTime());
    }

    @Test
    public void testGetMachineryList_all() {
        when(machineryMapper.selectList()).thenReturn(Arrays.asList(buildMachinery()));
        assertEquals(1, machineryService.getMachineryList().size());
    }

    @Test
    public void testGetMachineryList_idsEmpty() {
        assertTrue(machineryService.getMachineryList(Collections.emptyList()).isEmpty());
        verify(machineryMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetMachineryList_ids() {
        when(machineryMapper.selectByIds(any())).thenReturn(Arrays.asList(buildMachinery()));
        assertEquals(1, machineryService.getMachineryList(Arrays.asList(100L)).size());
    }

    // ========== importMachineryList ==========

    @Test
    public void testImportMachineryList_empty() {
        assertServiceException(() -> machineryService.importMachineryList(Collections.emptyList(), true),
                DV_MACHINERY_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    public void testImportMachineryList_create() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO()), true);
        assertEquals(1, respVO.getCreateCodes().size());
        assertEquals(0, respVO.getUpdateCodes().size());
        assertTrue(respVO.getFailureCodes().isEmpty());
        verify(machineryMapper).insert(any(MesDvMachineryDO.class));
        verify(barcodeService).autoGenerateBarcode(any(), eq(100L), eq("EQ-001"), eq("CNC 加工中心"));
    }

    @Test
    public void testImportMachineryList_updateSupport() {
        mockImportBaseData();
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(buildMachinery());
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO()), true);
        assertEquals(1, respVO.getUpdateCodes().size());
        verify(machineryMapper).updateById(any(MesDvMachineryDO.class));
    }

    @Test
    public void testImportMachineryList_noUpdateSupport() {
        mockImportBaseData();
        when(machineryMapper.selectByCode("EQ-001")).thenReturn(buildMachinery());
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO()), false);
        assertEquals(1, respVO.getFailureCodes().size());
        verify(machineryMapper, never()).updateById(any(MesDvMachineryDO.class));
    }

    @Test
    public void testImportMachineryList_blankCode() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setCode(null)), true);
        assertEquals(1, respVO.getFailureCodes().size());
        assertEquals("设备编码不能为空", respVO.getFailureCodes().get("第 1 行"));
    }

    @Test
    public void testImportMachineryList_blankName() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setName(null)), true);
        assertEquals(1, respVO.getFailureCodes().size());
        assertEquals("设备名称不能为空", respVO.getFailureCodes().get("EQ-001"));
    }

    @Test
    public void testImportMachineryList_blankMachineryTypeCode() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setMachineryTypeCode(null)), true);
        assertEquals(1, respVO.getFailureCodes().size());
        assertEquals("设备类型编码不能为空", respVO.getFailureCodes().get("EQ-001"));
    }

    @Test
    public void testImportMachineryList_machineryTypeNotExists() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setMachineryTypeCode("T999")), true);
        assertEquals(1, respVO.getFailureCodes().size());
        verify(machineryMapper, never()).insert(any(MesDvMachineryDO.class));
    }

    @Test
    public void testImportMachineryList_blankWorkshopCode() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setWorkshopCode(null)), true);
        assertEquals(1, respVO.getFailureCodes().size());
        assertEquals("车间编码不能为空", respVO.getFailureCodes().get("EQ-001"));
    }

    @Test
    public void testImportMachineryList_workshopNotExists() {
        mockImportBaseData();
        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(
                Arrays.asList(buildImportVO().setWorkshopCode("W999")), true);
        assertEquals(1, respVO.getFailureCodes().size());
        verify(machineryMapper, never()).insert(any(MesDvMachineryDO.class));
    }

}
