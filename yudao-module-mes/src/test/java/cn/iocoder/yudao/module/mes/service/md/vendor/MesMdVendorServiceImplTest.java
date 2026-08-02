package cn.iocoder.yudao.module.mes.service.md.vendor;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.md.vendor.vo.MesMdVendorImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.vendor.vo.MesMdVendorImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.vendor.vo.MesMdVendorPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.vendor.vo.MesMdVendorSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.vendor.MesMdVendorDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.vendor.MesMdVendorMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.qc.iqc.MesQcIqcService;
import cn.iocoder.yudao.module.mes.service.wm.arrivalnotice.MesWmArrivalNoticeService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.itemreceipt.MesWmItemReceiptService;
import cn.iocoder.yudao.module.mes.service.wm.outsourceissue.MesWmOutsourceIssueService;
import cn.iocoder.yudao.module.mes.service.wm.outsourcereceipt.MesWmOutsourceReceiptService;
import cn.iocoder.yudao.module.mes.service.wm.returnvendor.MesWmReturnVendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Resource;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesMdVendorServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesMdVendorServiceImpl.class)
public class MesMdVendorServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesMdVendorMapper vendorMapper;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesWmItemReceiptService itemReceiptService;
    @MockitoBean
    private MesWmArrivalNoticeService arrivalNoticeService;
    @MockitoBean
    private MesWmReturnVendorService returnVendorService;
    @MockitoBean
    private MesQcIqcService iqcService;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesWmOutsourceIssueService outsourceIssueService;
    @MockitoBean
    private MesWmOutsourceReceiptService outsourceReceiptService;

    @Resource
    private MesMdVendorServiceImpl vendorService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesMdVendorDO v = inv.getArgument(0);
            v.setId(v.getId() == null ? 100L : v.getId());
            return 1;
        }).when(vendorMapper).insert(any(MesMdVendorDO.class));
        when(vendorMapper.updateById(any(MesMdVendorDO.class))).thenReturn(1);
        when(vendorMapper.deleteById(anyLong())).thenReturn(1);
        // 引用计数默认 0
        when(itemReceiptService.getItemReceiptCountByVendorId(anyLong())).thenReturn(0L);
        when(arrivalNoticeService.getArrivalNoticeCountByVendorId(anyLong())).thenReturn(0L);
        when(returnVendorService.getReturnVendorCountByVendorId(anyLong())).thenReturn(0L);
        when(iqcService.getIqcCountByVendorId(anyLong())).thenReturn(0L);
        when(workOrderService.getWorkOrderCountByVendorId(anyLong())).thenReturn(0L);
        when(outsourceIssueService.getOutsourceIssueCountByVendorId(anyLong())).thenReturn(0L);
        when(outsourceReceiptService.getOutsourceReceiptCountByVendorId(anyLong())).thenReturn(0L);
    }

    private MesMdVendorDO buildVendor() {
        return new MesMdVendorDO().setId(100L).setCode("V001").setName("供应商A").setNickname("A")
                .setStatus(cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdVendorSaveReqVO buildSaveReq() {
        return new MesMdVendorSaveReqVO().setCode("V001").setName("供应商A").setNickname("A");
    }

    // ========== createVendor ==========

    @Test
    public void testCreateVendor_success() {
        Long id = vendorService.createVendor(buildSaveReq());
        assertEquals(100L, id);
        verify(vendorMapper).insert(any(MesMdVendorDO.class));
        verify(barcodeService).autoGenerateBarcode(any(), eq(100L), eq("V001"), eq("供应商A"));
    }

    @Test
    public void testCreateVendor_codeDuplicate() {
        when(vendorMapper.selectByCode("V001")).thenReturn(buildVendor());
        assertServiceException(() -> vendorService.createVendor(buildSaveReq()), MD_VENDOR_CODE_DUPLICATE);
    }

    @Test
    public void testCreateVendor_nameDuplicate() {
        when(vendorMapper.selectByName("供应商A")).thenReturn(buildVendor());
        assertServiceException(() -> vendorService.createVendor(buildSaveReq()), MD_VENDOR_NAME_DUPLICATE);
    }

    @Test
    public void testCreateVendor_nicknameDuplicate() {
        when(vendorMapper.selectByNickname("A")).thenReturn(buildVendor());
        assertServiceException(() -> vendorService.createVendor(buildSaveReq()), MD_VENDOR_NICKNAME_DUPLICATE);
    }

    // ========== updateVendor ==========

    @Test
    public void testUpdateVendor_success() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        MesMdVendorSaveReqVO req = buildSaveReq().setId(100L).setName("供应商B");
        vendorService.updateVendor(req);
        verify(vendorMapper).updateById(any(MesMdVendorDO.class));
    }

    @Test
    public void testUpdateVendor_notExists() {
        when(vendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> vendorService.updateVendor(buildSaveReq().setId(100L)), MD_VENDOR_NOT_EXISTS);
    }

    // ========== deleteVendor ==========

    @Test
    public void testDeleteVendor_success() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        vendorService.deleteVendor(100L);
        verify(vendorMapper).deleteById(100L);
    }

    @Test
    public void testDeleteVendor_notExists() {
        when(vendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> vendorService.deleteVendor(100L), MD_VENDOR_NOT_EXISTS);
    }

    @Test
    public void testDeleteVendor_hasReference() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        when(itemReceiptService.getItemReceiptCountByVendorId(100L)).thenReturn(1L);
        assertServiceException(() -> vendorService.deleteVendor(100L), MD_VENDOR_HAS_REFERENCE);
    }

    // ========== validateVendorExists ==========

    @Test
    public void testValidateVendorExists_exists() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        assertNotNull(vendorService.validateVendorExists(100L));
    }

    @Test
    public void testValidateVendorExists_notExists() {
        when(vendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> vendorService.validateVendorExists(100L), MD_VENDOR_NOT_EXISTS);
    }

    @Test
    public void testValidateVendorExistsAndEnable_enabled() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        assertNotNull(vendorService.validateVendorExistsAndEnable(100L));
    }

    @Test
    public void testValidateVendorExistsAndEnable_disabled() {
        MesMdVendorDO disabled = buildVendor().setStatus(
                cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.DISABLE.getStatus());
        when(vendorMapper.selectById(100L)).thenReturn(disabled);
        assertServiceException(() -> vendorService.validateVendorExistsAndEnable(100L), MD_VENDOR_IS_DISABLE);
    }

    // ========== get / list / page ==========

    @Test
    public void testGetVendor() {
        when(vendorMapper.selectById(100L)).thenReturn(buildVendor());
        assertNotNull(vendorService.getVendor(100L));
    }

    @Test
    public void testGetVendorPage() {
        PageResult<MesMdVendorDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(vendorMapper.selectPage(any())).thenReturn(page);
        assertEquals(0, vendorService.getVendorPage(new MesMdVendorPageReqVO()).getTotal());
    }

    @Test
    public void testGetVendorList_empty() {
        assertTrue(vendorService.getVendorList(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testGetVendorList_nonEmpty() {
        when(vendorMapper.selectByIds(any())).thenReturn(Arrays.asList(buildVendor()));
        assertEquals(1, vendorService.getVendorList(Arrays.asList(100L)).size());
    }

    // ========== importVendorList ==========

    @Test
    public void testImportVendorList_empty() {
        assertServiceException(() -> vendorService.importVendorList(Collections.emptyList(), true),
                MD_VENDOR_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    public void testImportVendorList_create() {
        MesMdVendorImportExcelVO vo = new MesMdVendorImportExcelVO().setCode("V001").setName("供应商A");
        MesMdVendorImportRespVO resp = vendorService.importVendorList(Arrays.asList(vo), true);
        assertEquals(1, resp.getCreateCodes().size());
        verify(vendorMapper).insert(any(MesMdVendorDO.class));
    }

    @Test
    public void testImportVendorList_updateSupport() {
        MesMdVendorImportExcelVO vo = new MesMdVendorImportExcelVO().setCode("V001").setName("供应商A");
        when(vendorMapper.selectByCode("V001")).thenReturn(buildVendor());
        MesMdVendorImportRespVO resp = vendorService.importVendorList(Arrays.asList(vo), true);
        assertEquals(1, resp.getUpdateCodes().size());
        verify(vendorMapper).updateById(any(MesMdVendorDO.class));
    }

    @Test
    public void testImportVendorList_noUpdateSupport_duplicate() {
        MesMdVendorImportExcelVO vo = new MesMdVendorImportExcelVO().setCode("V001").setName("供应商A");
        when(vendorMapper.selectByCode("V001")).thenReturn(buildVendor());
        MesMdVendorImportRespVO resp = vendorService.importVendorList(Arrays.asList(vo), false);
        assertEquals(1, resp.getFailureCodes().size());
        verify(vendorMapper, never()).updateById(any(MesMdVendorDO.class));
    }

    @Test
    public void testImportVendorList_blankCode() {
        MesMdVendorImportExcelVO vo = new MesMdVendorImportExcelVO().setName("供应商A");
        MesMdVendorImportRespVO resp = vendorService.importVendorList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
    }

    @Test
    public void testImportVendorList_blankName() {
        MesMdVendorImportExcelVO vo = new MesMdVendorImportExcelVO().setCode("V001");
        MesMdVendorImportRespVO resp = vendorService.importVendorList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
    }

}
