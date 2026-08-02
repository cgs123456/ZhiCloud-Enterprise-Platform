package cn.iocoder.yudao.module.mes.service.wm.returnissue;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.returnissue.vo.MesWmReturnIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.returnissue.vo.MesWmReturnIssueSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnissue.MesWmReturnIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnissue.MesWmReturnIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnissue.MesWmReturnIssueLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.returnissue.MesWmReturnIssueMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmQualityStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmReturnIssueStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import jakarta.annotation.Resource;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmReturnIssueServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmReturnIssueServiceImpl.class)
public class MesWmReturnIssueServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmReturnIssueMapper issueMapper;
    @MockitoBean
    private MesWmReturnIssueLineService issueLineService;
    @MockitoBean
    private MesWmReturnIssueDetailService issueDetailService;
    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesMdItemService itemService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;
    @MockitoBean
    private MesWmWarehouseService warehouseService;
    @MockitoBean
    private MesWmWarehouseLocationService locationService;
    @MockitoBean
    private MesWmWarehouseAreaService areaService;

    @Resource
    private MesWmReturnIssueServiceImpl issueService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmReturnIssueDO issue = inv.getArgument(0);
            issue.setId(issue.getId() == null ? 100L : issue.getId());
            return 1;
        }).when(issueMapper).insert(any(MesWmReturnIssueDO.class));
        when(issueMapper.updateById(any(MesWmReturnIssueDO.class))).thenReturn(1);
        when(issueMapper.deleteById(anyLong())).thenReturn(1);
        // 虚拟线边库
        when(warehouseService.getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(new MesWmWarehouseDO().setId(900L).setCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE));
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(new MesWmWarehouseLocationDO().setId(901L).setCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION));
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(new MesWmWarehouseAreaDO().setId(902L).setCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA));
        // 事务创建默认返回流水编号
        when(wmTransactionService.createTransaction(any(MesWmTransactionSaveReqDTO.class))).thenReturn(8000L);
    }

    private MesWmReturnIssueDO buildIssue(Integer status) {
        return new MesWmReturnIssueDO().setId(100L).setCode("RI001").setName("生产退料")
                .setWorkOrderId(1L).setWorkstationId(2L).setType(1).setStatus(status);
    }

    private MesWmReturnIssueSaveReqVO buildSaveReq() {
        MesWmReturnIssueSaveReqVO reqVO = new MesWmReturnIssueSaveReqVO();
        reqVO.setCode("RI001");
        reqVO.setName("生产退料");
        reqVO.setWorkOrderId(1L);
        reqVO.setType(1);
        reqVO.setReturnDate(LocalDateTime.now());
        reqVO.setRemark("备注");
        return reqVO;
    }

    private MesWmReturnIssueLineDO buildLine(Long id, Integer qualityStatus, String quantity) {
        return new MesWmReturnIssueLineDO().setId(id).setIssueId(100L).setItemId(10L)
                .setQuantity(new BigDecimal(quantity)).setQualityStatus(qualityStatus);
    }

    private MesWmReturnIssueDetailDO buildDetail(Long id, Long lineId, String quantity) {
        return new MesWmReturnIssueDetailDO().setId(id).setIssueId(100L).setLineId(lineId).setItemId(10L)
                .setQuantity(new BigDecimal(quantity)).setBatchId(20L).setBatchCode("B001")
                .setWarehouseId(30L).setLocationId(31L).setAreaId(32L);
    }

    // ========== createReturnIssue ==========

    @Test
    public void testCreateReturnIssue_success() {
        Long id = issueService.createReturnIssue(buildSaveReq());
        assertEquals(100L, id);
        verify(issueMapper).insert(any(MesWmReturnIssueDO.class));
        verify(workOrderService).validateWorkOrderConfirmed(1L);
        verify(workstationService, never()).validateWorkstationExistsAndEnable(anyLong());
    }

    @Test
    public void testCreateReturnIssue_withWorkstation() {
        MesWmReturnIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setWorkstationId(2L);
        issueService.createReturnIssue(reqVO);
        verify(workstationService).validateWorkstationExistsAndEnable(2L);
    }

    @Test
    public void testCreateReturnIssue_codeDuplicate() {
        when(issueMapper.selectByCode("RI001")).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> issueService.createReturnIssue(buildSaveReq()), WM_RETURN_ISSUE_CODE_DUPLICATE);
    }

    // ========== updateReturnIssue ==========

    @Test
    public void testUpdateReturnIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        MesWmReturnIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        issueService.updateReturnIssue(reqVO);
        verify(issueMapper).updateById(any(MesWmReturnIssueDO.class));
        verify(issueLineService, never()).updateReturnIssueQualityStatusByIssueId(anyLong(), anyInt());
    }

    @Test
    public void testUpdateReturnIssue_typeChanged() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        MesWmReturnIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setType(2);
        issueService.updateReturnIssue(reqVO);
        verify(issueLineService).updateReturnIssueQualityStatusByIssueId(100L, 2);
    }

    @Test
    public void testUpdateReturnIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        MesWmReturnIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> issueService.updateReturnIssue(reqVO), WM_RETURN_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testUpdateReturnIssue_notPrepare() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.FINISHED.getStatus()));
        MesWmReturnIssueSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> issueService.updateReturnIssue(reqVO), WM_RETURN_ISSUE_NOT_PREPARE);
    }

    // ========== deleteReturnIssue ==========

    @Test
    public void testDeleteReturnIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        issueService.deleteReturnIssue(100L);
        verify(issueDetailService).deleteReturnIssueDetailByIssueId(100L);
        verify(issueLineService).deleteReturnIssueLineByIssueId(100L);
        verify(issueMapper).deleteById(100L);
    }

    @Test
    public void testDeleteReturnIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.deleteReturnIssue(100L), WM_RETURN_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testDeleteReturnIssue_notPrepare() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> issueService.deleteReturnIssue(100L), WM_RETURN_ISSUE_NOT_PREPARE);
    }

    // ========== get / page ==========

    @Test
    public void testGetReturnIssue() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        assertNotNull(issueService.getReturnIssue(100L));
    }

    @Test
    public void testGetReturnIssue_null() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertNull(issueService.getReturnIssue(100L));
    }

    @Test
    public void testGetReturnIssuePage() {
        PageResult<MesWmReturnIssueDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(issueMapper.selectPage(any(MesWmReturnIssuePageReqVO.class))).thenReturn(page);
        assertEquals(0, issueService.getReturnIssuePage(new MesWmReturnIssuePageReqVO()).getTotal());
    }

    // ========== submitReturnIssue ==========

    @Test
    public void testSubmitReturnIssue_noLine() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> issueService.submitReturnIssue(100L), WM_RETURN_ISSUE_NO_LINE);
    }

    @Test
    public void testSubmitReturnIssue_hasPendingQc() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(
                buildLine(1L, MesWmQualityStatusEnum.PENDING.getStatus(), "10"),
                buildLine(2L, MesWmQualityStatusEnum.PASS.getStatus(), "5")));
        issueService.submitReturnIssue(100L);
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.CONFIRMED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testSubmitReturnIssue_noPendingQc() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        issueService.submitReturnIssue(100L);
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.APPROVING.getStatus(), captor.getValue().getStatus());
    }

    // ========== stockReturnIssue ==========

    @Test
    public void testStockReturnIssue_notApproving() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> issueService.stockReturnIssue(100L), WM_RETURN_ISSUE_NOT_APPROVING);
    }

    @Test
    public void testStockReturnIssue_noLine() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVING.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        issueService.stockReturnIssue(100L);
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.APPROVED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testStockReturnIssue_quantityMatch() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVING.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(issueDetailService.getReturnIssueDetailListByIssueId(100L)).thenReturn(Arrays.asList(
                buildDetail(11L, 1L, "6"), buildDetail(12L, 1L, "4")));
        issueService.stockReturnIssue(100L);
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.APPROVED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testStockReturnIssue_quantityMismatch() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVING.getStatus()));
        when(issueLineService.getReturnIssueLineListByIssueId(100L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(issueDetailService.getReturnIssueDetailListByIssueId(100L)).thenReturn(Collections.singletonList(
                buildDetail(11L, 1L, "3")));
        when(itemService.validateItemExistsAndEnable(10L))
                .thenReturn(new MesMdItemDO().setId(10L).setCode("I001").setName("物料A"));
        assertServiceException(() -> issueService.stockReturnIssue(100L), WM_RETURN_ISSUE_DETAIL_QUANTITY_MISMATCH);
        verify(issueMapper, never()).updateById(any(MesWmReturnIssueDO.class));
    }

    // ========== finishReturnIssue ==========

    @Test
    public void testFinishReturnIssue_notApproved() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> issueService.finishReturnIssue(100L), WM_RETURN_ISSUE_NOT_APPROVED);
    }

    @Test
    public void testFinishReturnIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVED.getStatus()));
        when(issueDetailService.getReturnIssueDetailListByIssueId(100L)).thenReturn(Collections.singletonList(
                buildDetail(11L, 1L, "10")));
        issueService.finishReturnIssue(100L);
        // 每条明细产生 OUT + IN 两条事务
        verify(wmTransactionService, times(2)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.FINISHED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testFinishReturnIssue_noDetail() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.APPROVED.getStatus()));
        when(issueDetailService.getReturnIssueDetailListByIssueId(100L)).thenReturn(Collections.emptyList());
        issueService.finishReturnIssue(100L);
        verify(wmTransactionService, never()).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(issueMapper).updateById(any(MesWmReturnIssueDO.class));
    }

    // ========== cancelReturnIssue ==========

    @Test
    public void testCancelReturnIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        issueService.cancelReturnIssue(100L);
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.CANCELED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testCancelReturnIssue_finished() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> issueService.cancelReturnIssue(100L), WM_RETURN_ISSUE_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelReturnIssue_canceled() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> issueService.cancelReturnIssue(100L), WM_RETURN_ISSUE_CANCEL_NOT_ALLOWED);
    }

    // ========== validate / updateStatus ==========

    @Test
    public void testValidateReturnIssueExists_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        assertNotNull(issueService.validateReturnIssueExists(100L));
    }

    @Test
    public void testValidateReturnIssueExists_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.validateReturnIssueExists(100L), WM_RETURN_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testValidateReturnIssueExistsAndPrepare_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        assertNotNull(issueService.validateReturnIssueExistsAndPrepare(100L));
    }

    @Test
    public void testValidateReturnIssueExistsAndPrepare_notPrepare() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.CONFIRMED.getStatus()));
        assertServiceException(() -> issueService.validateReturnIssueExistsAndPrepare(100L), WM_RETURN_ISSUE_NOT_PREPARE);
    }

    @Test
    public void testUpdateReturnIssueStatus_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmReturnIssueStatusEnum.PREPARE.getStatus()));
        issueService.updateReturnIssueStatus(100L, MesWmReturnIssueStatusEnum.APPROVING.getStatus());
        ArgumentCaptor<MesWmReturnIssueDO> captor = ArgumentCaptor.forClass(MesWmReturnIssueDO.class);
        verify(issueMapper).updateById(captor.capture());
        assertEquals(MesWmReturnIssueStatusEnum.APPROVING.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testUpdateReturnIssueStatus_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.updateReturnIssueStatus(100L,
                MesWmReturnIssueStatusEnum.APPROVING.getStatus()), WM_RETURN_ISSUE_NOT_EXISTS);
    }

}
