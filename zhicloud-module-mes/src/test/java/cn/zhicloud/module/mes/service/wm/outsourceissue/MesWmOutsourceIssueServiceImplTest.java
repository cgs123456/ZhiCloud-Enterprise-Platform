package cn.zhicloud.module.mes.service.wm.outsourceissue;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.outsourceissue.vo.MesWmOutsourceIssuePageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.outsourceissue.vo.MesWmOutsourceIssueSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.vendor.MesMdVendorDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueLineDO;
import cn.zhicloud.module.mes.dal.mysql.wm.outsourceissue.MesWmOutsourceIssueMapper;
import cn.zhicloud.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmOutsourceIssueStatusEnum;
import cn.zhicloud.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.zhicloud.module.mes.service.md.vendor.MesMdVendorService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.zhicloud.module.mes.service.wm.transaction.MesWmTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import jakarta.annotation.Resource;
import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmOutsourceIssueServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmOutsourceIssueServiceImpl.class)
public class MesWmOutsourceIssueServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmOutsourceIssueMapper outsourceIssueMapper;
    @MockitoBean
    private MesWmOutsourceIssueLineService outsourceIssueLineService;
    @MockitoBean
    private MesWmOutsourceIssueDetailService outsourceIssueDetailService;
    @MockitoBean
    private MesMdVendorService vendorService;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;
    @MockitoBean
    private MesMdAutoCodeRecordService autoCodeRecordService;

    @Resource
    private MesWmOutsourceIssueServiceImpl outsourceIssueService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmOutsourceIssueDO issue = inv.getArgument(0);
            issue.setId(issue.getId() == null ? 100L : issue.getId());
            return 1;
        }).when(outsourceIssueMapper).insert(any(MesWmOutsourceIssueDO.class));
        when(outsourceIssueMapper.updateById(any(MesWmOutsourceIssueDO.class))).thenReturn(1);
        when(outsourceIssueMapper.deleteById(anyLong())).thenReturn(1);
        // 工单默认为外协（代工）类型
        when(workOrderService.validateWorkOrderConfirmed(anyLong())).thenReturn(new MesProWorkOrderDO()
                .setId(200L).setType(MesProWorkOrderTypeEnum.OUTSOURCE.getType()));
        // 供应商默认存在且启用
        when(vendorService.validateVendorExistsAndEnable(anyLong())).thenReturn(new MesMdVendorDO().setId(300L));
    }

    private MesWmOutsourceIssueDO buildIssue(Integer status) {
        return new MesWmOutsourceIssueDO().setId(100L).setCode("WOS001").setName("外协发料单A")
                .setVendorId(300L).setWorkOrderId(200L).setIssueDate(LocalDateTime.now()).setStatus(status);
    }

    private MesWmOutsourceIssueSaveReqVO buildSaveReq() {
        MesWmOutsourceIssueSaveReqVO reqVO = new MesWmOutsourceIssueSaveReqVO();
        reqVO.setCode("WOS001");
        reqVO.setName("外协发料单A");
        reqVO.setWorkOrderId(200L);
        reqVO.setIssueDate(LocalDateTime.now());
        reqVO.setRemark("备注");
        return reqVO;
    }

    private MesWmOutsourceIssueLineDO buildLine(Long id, BigDecimal quantity) {
        return new MesWmOutsourceIssueLineDO().setId(id).setIssueId(100L).setItemId(400L).setQuantity(quantity);
    }

    private MesWmOutsourceIssueDetailDO buildDetail(Long id, Long lineId, BigDecimal quantity) {
        return new MesWmOutsourceIssueDetailDO().setId(id).setLineId(lineId).setIssueId(100L)
                .setItemId(400L).setQuantity(quantity).setBatchId(500L)
                .setWarehouseId(600L).setLocationId(700L).setAreaId(800L);
    }

    // ========== createOutsourceIssue ==========

    @Test
    public void testCreateOutsourceIssue_success() {
        Long id = outsourceIssueService.createOutsourceIssue(buildSaveReq());
        assertEquals(100L, id);
        verify(outsourceIssueMapper).insert(any(MesWmOutsourceIssueDO.class));
        verify(autoCodeRecordService, never()).generateAutoCode(anyString());
    }

    @Test
    public void testCreateOutsourceIssue_autoGenerateCode() {
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.WM_OUTSOURCE_ISSUE_CODE.getCode()))
                .thenReturn("WOS202603020001");
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setCode(null);

        Long id = outsourceIssueService.createOutsourceIssue(reqVO);
        assertEquals(100L, id);
        assertEquals("WOS202603020001", reqVO.getCode());
        verify(autoCodeRecordService).generateAutoCode(
                MesMdAutoCodeRuleCodeEnum.WM_OUTSOURCE_ISSUE_CODE.getCode());
    }

    @Test
    public void testCreateOutsourceIssue_withVendor() {
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setVendorId(300L);

        outsourceIssueService.createOutsourceIssue(reqVO);
        verify(vendorService).validateVendorExistsAndEnable(300L);
    }

    @Test
    public void testCreateOutsourceIssue_withoutVendor() {
        outsourceIssueService.createOutsourceIssue(buildSaveReq());
        verify(vendorService, never()).validateVendorExistsAndEnable(anyLong());
    }

    @Test
    public void testCreateOutsourceIssue_codeDuplicate() {
        when(outsourceIssueMapper.selectByCode("WOS001"))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> outsourceIssueService.createOutsourceIssue(buildSaveReq()),
                WM_OUTSOURCE_ISSUE_CODE_DUPLICATE);
    }

    @Test
    public void testCreateOutsourceIssue_workOrderTypeInvalid() {
        when(workOrderService.validateWorkOrderConfirmed(200L)).thenReturn(new MesProWorkOrderDO()
                .setId(200L).setType(MesProWorkOrderTypeEnum.SELF.getType()));
        assertServiceException(() -> outsourceIssueService.createOutsourceIssue(buildSaveReq()),
                WM_OUTSOURCE_ISSUE_WORK_ORDER_TYPE_INVALID);
    }

    // ========== updateOutsourceIssue ==========

    @Test
    public void testUpdateOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setName("外协发料单B");

        outsourceIssueService.updateOutsourceIssue(reqVO);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testUpdateOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> outsourceIssueService.updateOutsourceIssue(reqVO),
                WM_OUTSOURCE_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testUpdateOutsourceIssue_statusNotPrepare() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVING.getStatus()));
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> outsourceIssueService.updateOutsourceIssue(reqVO),
                WM_OUTSOURCE_ISSUE_STATUS_NOT_PREPARE);
    }

    @Test
    public void testUpdateOutsourceIssue_codeDuplicate() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        when(outsourceIssueMapper.selectByCode("WOS001"))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()).setId(101L));
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> outsourceIssueService.updateOutsourceIssue(reqVO),
                WM_OUTSOURCE_ISSUE_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateOutsourceIssue_sameCodeSameId() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        when(outsourceIssueMapper.selectByCode("WOS001"))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        MesWmOutsourceIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);

        outsourceIssueService.updateOutsourceIssue(reqVO);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    // ========== deleteOutsourceIssue ==========

    @Test
    public void testDeleteOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));

        outsourceIssueService.deleteOutsourceIssue(100L);
        verify(outsourceIssueLineService).deleteOutsourceIssueLineByIssueId(100L);
        verify(outsourceIssueDetailService).deleteOutsourceIssueDetailByIssueId(100L);
        verify(outsourceIssueMapper).deleteById(100L);
    }

    @Test
    public void testDeleteOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> outsourceIssueService.deleteOutsourceIssue(100L), WM_OUTSOURCE_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testDeleteOutsourceIssue_statusNotPrepare() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> outsourceIssueService.deleteOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_STATUS_NOT_PREPARE);
    }

    // ========== get / page ==========

    @Test
    public void testGetOutsourceIssue() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        assertNotNull(outsourceIssueService.getOutsourceIssue(100L));
    }

    @Test
    public void testGetOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        assertNull(outsourceIssueService.getOutsourceIssue(100L));
    }

    @Test
    public void testGetOutsourceIssuePage() {
        PageResult<MesWmOutsourceIssueDO> page = new PageResult<>(
                Arrays.asList(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus())), 1L);
        when(outsourceIssueMapper.selectPage(any(MesWmOutsourceIssuePageReqVO.class))).thenReturn(page);

        PageResult<MesWmOutsourceIssueDO> result = outsourceIssueService.getOutsourceIssuePage(
                new MesWmOutsourceIssuePageReqVO());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    // ========== submitOutsourceIssue ==========

    @Test
    public void testSubmitOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));

        outsourceIssueService.submitOutsourceIssue(100L);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testSubmitOutsourceIssue_noLine() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> outsourceIssueService.submitOutsourceIssue(100L), WM_OUTSOURCE_ISSUE_NO_LINE);
    }

    @Test
    public void testSubmitOutsourceIssue_statusNotPrepare() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> outsourceIssueService.submitOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_STATUS_NOT_PREPARE);
    }

    // ========== stockOutsourceIssue ==========

    @Test
    public void testStockOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVING.getStatus()));
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4")),
                        buildDetail(12L, 1L, new BigDecimal("6"))));

        outsourceIssueService.stockOutsourceIssue(100L);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testStockOutsourceIssue_noLine() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVING.getStatus()));
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());

        outsourceIssueService.stockOutsourceIssue(100L);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testStockOutsourceIssue_quantityMismatch() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVING.getStatus()));
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4"))));
        assertServiceException(() -> outsourceIssueService.stockOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_QUANTITY_MISMATCH);
    }

    @Test
    public void testStockOutsourceIssue_statusNotApproving() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> outsourceIssueService.stockOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_STATUS_NOT_APPROVING);
    }

    @Test
    public void testStockOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> outsourceIssueService.stockOutsourceIssue(100L), WM_OUTSOURCE_ISSUE_NOT_EXISTS);
    }

    // ========== finishOutsourceIssue ==========

    @Test
    public void testFinishOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVED.getStatus()));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("10"))));

        outsourceIssueService.finishOutsourceIssue(100L);
        verify(wmTransactionService).createTransactionList(anyList());
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testFinishOutsourceIssue_statusNotApproved() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> outsourceIssueService.finishOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_STATUS_NOT_APPROVED);
    }

    @Test
    public void testFinishOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> outsourceIssueService.finishOutsourceIssue(100L), WM_OUTSOURCE_ISSUE_NOT_EXISTS);
    }

    // ========== cancelOutsourceIssue ==========

    @Test
    public void testCancelOutsourceIssue_success() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.PREPARE.getStatus()));

        outsourceIssueService.cancelOutsourceIssue(100L);
        verify(outsourceIssueMapper).updateById(any(MesWmOutsourceIssueDO.class));
    }

    @Test
    public void testCancelOutsourceIssue_finishedNotAllowed() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> outsourceIssueService.cancelOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelOutsourceIssue_cancelledNotAllowed() {
        when(outsourceIssueMapper.selectById(100L))
                .thenReturn(buildIssue(MesWmOutsourceIssueStatusEnum.CANCELLED.getStatus()));
        assertServiceException(() -> outsourceIssueService.cancelOutsourceIssue(100L),
                WM_OUTSOURCE_ISSUE_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelOutsourceIssue_notExists() {
        when(outsourceIssueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> outsourceIssueService.cancelOutsourceIssue(100L), WM_OUTSOURCE_ISSUE_NOT_EXISTS);
    }

    // ========== checkOutsourceIssueQuantity ==========

    @Test
    public void testCheckOutsourceIssueQuantity_noLine() {
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertTrue(outsourceIssueService.checkOutsourceIssueQuantity(100L));
    }

    @Test
    public void testCheckOutsourceIssueQuantity_match() {
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4")),
                        buildDetail(12L, 1L, new BigDecimal("6"))));
        assertTrue(outsourceIssueService.checkOutsourceIssueQuantity(100L));
    }

    @Test
    public void testCheckOutsourceIssueQuantity_mismatch() {
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4"))));
        assertFalse(outsourceIssueService.checkOutsourceIssueQuantity(100L));
    }

    @Test
    public void testCheckOutsourceIssueQuantity_noDetail() {
        when(outsourceIssueLineService.getOutsourceIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(outsourceIssueDetailService.getOutsourceIssueDetailListByIssueId(100L))
                .thenReturn(Collections.emptyList());
        assertFalse(outsourceIssueService.checkOutsourceIssueQuantity(100L));
    }

    // ========== getOutsourceIssueCountByVendorId ==========

    @Test
    public void testGetOutsourceIssueCountByVendorId() {
        when(outsourceIssueMapper.selectCountByVendorId(300L)).thenReturn(5L);
        assertEquals(5L, outsourceIssueService.getOutsourceIssueCountByVendorId(300L));
    }

    @Test
    public void testGetOutsourceIssueCountByVendorId_zero() {
        when(outsourceIssueMapper.selectCountByVendorId(anyLong())).thenReturn(0L);
        assertEquals(0L, outsourceIssueService.getOutsourceIssueCountByVendorId(999L));
    }

}
