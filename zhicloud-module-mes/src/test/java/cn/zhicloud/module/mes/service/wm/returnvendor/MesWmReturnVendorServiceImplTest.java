package cn.zhicloud.module.mes.service.wm.returnvendor;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.returnvendor.vo.MesWmReturnVendorPageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.returnvendor.vo.MesWmReturnVendorSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.returnvendor.MesWmReturnVendorDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.returnvendor.MesWmReturnVendorDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.returnvendor.MesWmReturnVendorLineDO;
import cn.zhicloud.module.mes.dal.mysql.wm.returnvendor.MesWmReturnVendorMapper;
import cn.zhicloud.module.mes.enums.wm.MesWmReturnVendorStatusEnum;
import cn.zhicloud.module.mes.service.md.vendor.MesMdVendorService;
import cn.zhicloud.module.mes.service.wm.transaction.MesWmTransactionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmReturnVendorServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmReturnVendorServiceImpl.class)
public class MesWmReturnVendorServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmReturnVendorMapper returnVendorMapper;
    @MockitoBean
    private MesWmReturnVendorLineService returnVendorLineService;
    @MockitoBean
    private MesWmReturnVendorDetailService returnVendorDetailService;
    @MockitoBean
    private MesMdVendorService vendorService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;

    @Resource
    private MesWmReturnVendorServiceImpl returnVendorService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmReturnVendorDO returnVendor = inv.getArgument(0);
            returnVendor.setId(returnVendor.getId() == null ? 100L : returnVendor.getId());
            return 1;
        }).when(returnVendorMapper).insert(any(MesWmReturnVendorDO.class));
        when(returnVendorMapper.updateById(any(MesWmReturnVendorDO.class))).thenReturn(1);
        when(returnVendorMapper.deleteById(anyLong())).thenReturn(1);
        // 默认编码不重复
        when(returnVendorMapper.selectByCode(anyString())).thenReturn(null);
    }

    private MesWmReturnVendorDO buildReturnVendor(Integer status) {
        return new MesWmReturnVendorDO().setId(100L).setCode("RV001").setName("供应商退货单")
                .setVendorId(200L).setPurchaseOrderCode("PO001")
                .setReturnDate(LocalDateTime.now()).setStatus(status);
    }

    private MesWmReturnVendorSaveReqVO buildSaveReq() {
        return new MesWmReturnVendorSaveReqVO().setCode("RV001").setName("供应商退货单")
                .setVendorId(200L).setReturnDate(LocalDateTime.now());
    }

    private MesWmReturnVendorLineDO buildLine(Long id, BigDecimal quantity) {
        return new MesWmReturnVendorLineDO().setId(id).setReturnId(100L).setItemId(300L).setQuantity(quantity);
    }

    private MesWmReturnVendorDetailDO buildDetail(Long id, Long lineId, BigDecimal quantity) {
        return new MesWmReturnVendorDetailDO().setId(id).setReturnId(100L).setLineId(lineId)
                .setItemId(300L).setQuantity(quantity).setBatchId(400L).setBatchCode("B001")
                .setWarehouseId(10L).setLocationId(20L).setAreaId(30L).setMaterialStockId(50L);
    }

    // ========== createReturnVendor ==========

    @Test
    public void testCreateReturnVendor_success() {
        Long id = returnVendorService.createReturnVendor(buildSaveReq());
        assertEquals(100L, id);
        verify(returnVendorMapper).insert(any(MesWmReturnVendorDO.class));
        verify(vendorService).validateVendorExistsAndEnable(200L);
    }

    @Test
    public void testCreateReturnVendor_codeDuplicate() {
        when(returnVendorMapper.selectByCode("RV001")).thenReturn(buildReturnVendor(
                MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> returnVendorService.createReturnVendor(buildSaveReq()),
                WM_RETURN_VENDOR_CODE_DUPLICATE);
    }

    // ========== updateReturnVendor ==========

    @Test
    public void testUpdateReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        returnVendorService.updateReturnVendor(buildSaveReq().setId(100L).setName("新名称"));
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    @Test
    public void testUpdateReturnVendor_notExists() {
        when(returnVendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> returnVendorService.updateReturnVendor(buildSaveReq().setId(100L)),
                WM_RETURN_VENDOR_NOT_EXISTS);
    }

    @Test
    public void testUpdateReturnVendor_statusInvalid() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> returnVendorService.updateReturnVendor(buildSaveReq().setId(100L)),
                WM_RETURN_VENDOR_STATUS_INVALID);
    }

    @Test
    public void testUpdateReturnVendor_codeDuplicate() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        when(returnVendorMapper.selectByCode("RV001")).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()).setId(999L));
        assertServiceException(() -> returnVendorService.updateReturnVendor(buildSaveReq().setId(100L)),
                WM_RETURN_VENDOR_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateReturnVendor_codeSelfNotDuplicate() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        when(returnVendorMapper.selectByCode("RV001")).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        returnVendorService.updateReturnVendor(buildSaveReq().setId(100L));
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    // ========== deleteReturnVendor ==========

    @Test
    public void testDeleteReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        returnVendorService.deleteReturnVendor(100L);
        verify(returnVendorDetailService).deleteReturnVendorDetailByReturnId(100L);
        verify(returnVendorLineService).deleteReturnVendorLineByReturnId(100L);
        verify(returnVendorMapper).deleteById(100L);
    }

    @Test
    public void testDeleteReturnVendor_notExists() {
        when(returnVendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> returnVendorService.deleteReturnVendor(100L), WM_RETURN_VENDOR_NOT_EXISTS);
    }

    @Test
    public void testDeleteReturnVendor_statusInvalid() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> returnVendorService.deleteReturnVendor(100L), WM_RETURN_VENDOR_STATUS_INVALID);
    }

    // ========== get / page ==========

    @Test
    public void testGetReturnVendor() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnVendorService.getReturnVendor(100L));
    }

    @Test
    public void testGetReturnVendor_null() {
        when(returnVendorMapper.selectById(100L)).thenReturn(null);
        assertNull(returnVendorService.getReturnVendor(100L));
    }

    @Test
    public void testGetReturnVendorPage() {
        PageResult<MesWmReturnVendorDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(returnVendorMapper.selectPage(any(MesWmReturnVendorPageReqVO.class))).thenReturn(page);
        assertEquals(0L, returnVendorService.getReturnVendorPage(new MesWmReturnVendorPageReqVO()).getTotal());
    }

    // ========== submitReturnVendor ==========

    @Test
    public void testSubmitReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        returnVendorService.submitReturnVendor(100L);
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    @Test
    public void testSubmitReturnVendor_noLine() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Collections.emptyList());
        assertServiceException(() -> returnVendorService.submitReturnVendor(100L), WM_RETURN_VENDOR_NO_LINE);
    }

    @Test
    public void testSubmitReturnVendor_statusInvalid() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> returnVendorService.submitReturnVendor(100L), WM_RETURN_VENDOR_STATUS_INVALID);
    }

    // ========== stockReturnVendor ==========

    @Test
    public void testStockReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVING.getStatus()));
        returnVendorService.stockReturnVendor(100L);
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    @Test
    public void testStockReturnVendor_statusInvalid() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> returnVendorService.stockReturnVendor(100L), WM_RETURN_VENDOR_STATUS_INVALID);
    }

    @Test
    public void testStockReturnVendor_notExists() {
        when(returnVendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> returnVendorService.stockReturnVendor(100L), WM_RETURN_VENDOR_NOT_EXISTS);
    }

    // ========== finishReturnVendor ==========

    @Test
    public void testFinishReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVED.getStatus()));
        when(returnVendorDetailService.getReturnVendorDetailListByReturnId(100L))
                .thenReturn(Arrays.asList(buildDetail(1L, 1L, new BigDecimal("10"))));
        returnVendorService.finishReturnVendor(100L);
        verify(wmTransactionService).createTransactionList(anyList());
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    @Test
    public void testFinishReturnVendor_statusInvalid() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> returnVendorService.finishReturnVendor(100L), WM_RETURN_VENDOR_STATUS_INVALID);
    }

    @Test
    public void testFinishReturnVendor_noDetail() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVED.getStatus()));
        when(returnVendorDetailService.getReturnVendorDetailListByReturnId(100L))
                .thenReturn(Collections.emptyList());
        assertServiceException(() -> returnVendorService.finishReturnVendor(100L), WM_RETURN_VENDOR_NO_DETAIL);
    }

    // ========== cancelReturnVendor ==========

    @Test
    public void testCancelReturnVendor_success() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.APPROVING.getStatus()));
        returnVendorService.cancelReturnVendor(100L);
        verify(returnVendorMapper).updateById(any(MesWmReturnVendorDO.class));
    }

    @Test
    public void testCancelReturnVendor_finishedNotAllowed() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> returnVendorService.cancelReturnVendor(100L),
                WM_RETURN_VENDOR_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelReturnVendor_canceledNotAllowed() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> returnVendorService.cancelReturnVendor(100L),
                WM_RETURN_VENDOR_CANCEL_NOT_ALLOWED);
    }

    // ========== checkReturnVendorQuantity ==========

    @Test
    public void testCheckReturnVendorQuantity_match() {
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(returnVendorDetailService.getReturnVendorDetailListByLineId(1L))
                .thenReturn(Arrays.asList(buildDetail(1L, 1L, new BigDecimal("6")),
                        buildDetail(2L, 1L, new BigDecimal("4"))));
        assertTrue(returnVendorService.checkReturnVendorQuantity(100L));
    }

    @Test
    public void testCheckReturnVendorQuantity_mismatch() {
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(returnVendorDetailService.getReturnVendorDetailListByLineId(1L))
                .thenReturn(Arrays.asList(buildDetail(1L, 1L, new BigDecimal("3"))));
        assertFalse(returnVendorService.checkReturnVendorQuantity(100L));
    }

    @Test
    public void testCheckReturnVendorQuantity_lineQuantityNull() {
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, null)));
        when(returnVendorDetailService.getReturnVendorDetailListByLineId(1L))
                .thenReturn(Collections.emptyList());
        assertTrue(returnVendorService.checkReturnVendorQuantity(100L));
    }

    @Test
    public void testCheckReturnVendorQuantity_noLine() {
        when(returnVendorLineService.getReturnVendorLineListByReturnId(100L))
                .thenReturn(Collections.emptyList());
        assertTrue(returnVendorService.checkReturnVendorQuantity(100L));
    }

    // ========== validate ==========

    @Test
    public void testValidateReturnVendorExists_exists() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnVendorService.validateReturnVendorExists(100L));
    }

    @Test
    public void testValidateReturnVendorExists_notExists() {
        when(returnVendorMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> returnVendorService.validateReturnVendorExists(100L),
                WM_RETURN_VENDOR_NOT_EXISTS);
    }

    @Test
    public void testValidateReturnVendorExistsAndPrepare_prepare() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnVendorService.validateReturnVendorExistsAndPrepare(100L));
    }

    @Test
    public void testValidateReturnVendorExistsAndPrepare_notPrepare() {
        when(returnVendorMapper.selectById(100L)).thenReturn(
                buildReturnVendor(MesWmReturnVendorStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> returnVendorService.validateReturnVendorExistsAndPrepare(100L),
                WM_RETURN_VENDOR_STATUS_INVALID);
    }

    // ========== getReturnVendorCountByVendorId ==========

    @Test
    public void testGetReturnVendorCountByVendorId() {
        when(returnVendorMapper.selectCountByVendorId(200L)).thenReturn(3L);
        assertEquals(3L, returnVendorService.getReturnVendorCountByVendorId(200L));
    }

    @Test
    public void testGetReturnVendorCountByVendorId_zero() {
        when(returnVendorMapper.selectCountByVendorId(anyLong())).thenReturn(0L);
        List<Long> vendorIds = Arrays.asList(1L, 2L);
        for (Long vendorId : vendorIds) {
            assertEquals(0L, returnVendorService.getReturnVendorCountByVendorId(vendorId));
        }
    }

}
