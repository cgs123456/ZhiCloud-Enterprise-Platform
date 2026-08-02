package cn.iocoder.yudao.module.mes.service.wm.productissue;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.productissue.vo.MesWmProductIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.productissue.vo.MesWmProductIssueSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmProductIssueServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmProductIssueServiceImpl.class)
public class MesWmProductIssueServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmProductIssueMapper issueMapper;
    @MockitoBean
    private MesWmProductIssueLineService issueLineService;
    @MockitoBean
    private MesWmProductIssueDetailService issueDetailService;
    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;
    @MockitoBean
    private MesWmWarehouseService warehouseService;
    @MockitoBean
    private MesWmWarehouseLocationService locationService;
    @MockitoBean
    private MesWmWarehouseAreaService areaService;

    @Resource
    private MesWmProductIssueServiceImpl issueService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmProductIssueDO issue = inv.getArgument(0);
            issue.setId(issue.getId() == null ? 100L : issue.getId());
            return 1;
        }).when(issueMapper).insert(any(MesWmProductIssueDO.class));
        when(issueMapper.updateById(any(MesWmProductIssueDO.class))).thenReturn(1);
        when(issueMapper.deleteById(anyLong())).thenReturn(1);
        // 虚拟线边库
        when(warehouseService.getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(new MesWmWarehouseDO().setId(11L));
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(new MesWmWarehouseLocationDO().setId(12L));
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(new MesWmWarehouseAreaDO().setId(13L));
        // 库存事务默认返回一个 ID
        when(wmTransactionService.createTransaction(any(MesWmTransactionSaveReqDTO.class))).thenReturn(9527L);
    }

    // ==================== 构造数据 ====================

    private MesWmProductIssueDO buildIssue(Integer status) {
        return new MesWmProductIssueDO().setId(100L).setCode("ISSUE001").setName("生产领料")
                .setWorkOrderId(1000L).setWorkstationId(2000L)
                .setRequiredTime(LocalDateTime.now()).setStatus(status);
    }

    private MesWmProductIssueSaveReqVO buildSaveReq() {
        return new MesWmProductIssueSaveReqVO().setCode("ISSUE001").setName("生产领料")
                .setWorkOrderId(1000L).setRequiredTime(LocalDateTime.now());
    }

    private MesWmProductIssueLineDO buildLine() {
        return new MesWmProductIssueLineDO().setId(200L).setIssueId(100L).setItemId(400L)
                .setQuantity(new BigDecimal("10")).setBatchId(500L);
    }

    private MesWmProductIssueDetailDO buildDetail(BigDecimal quantity) {
        return new MesWmProductIssueDetailDO().setId(600L).setIssueId(100L).setLineId(200L)
                .setMaterialStockId(700L).setItemId(400L).setQuantity(quantity)
                .setBatchId(500L).setBatchCode("B001")
                .setWarehouseId(1L).setLocationId(2L).setAreaId(3L);
    }

    // ==================== createProductIssue ====================

    @Test
    public void testCreateProductIssue_success() {
        Long id = issueService.createProductIssue(buildSaveReq());
        assertEquals(100L, id);
        verify(issueMapper).insert(any(MesWmProductIssueDO.class));
        verify(workOrderService).validateWorkOrderConfirmed(1000L);
        verify(workstationService, never()).validateWorkstationExistsAndEnable(anyLong());
    }

    @Test
    public void testCreateProductIssue_withWorkstation() {
        Long id = issueService.createProductIssue(buildSaveReq().setWorkstationId(2000L));
        assertEquals(100L, id);
        verify(workstationService).validateWorkstationExistsAndEnable(2000L);
    }

    @Test
    public void testCreateProductIssue_codeDuplicate() {
        when(issueMapper.selectByCode("ISSUE001"))
                .thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> issueService.createProductIssue(buildSaveReq()),
                WM_PRODUCT_ISSUE_CODE_DUPLICATE);
    }

    // ==================== updateProductIssue ====================

    @Test
    public void testUpdateProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        issueService.updateProductIssue(buildSaveReq().setId(100L).setName("生产领料B"));
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testUpdateProductIssue_codeSelf() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        when(issueMapper.selectByCode("ISSUE001"))
                .thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        issueService.updateProductIssue(buildSaveReq().setId(100L));
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testUpdateProductIssue_codeDuplicate() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        when(issueMapper.selectByCode("ISSUE001"))
                .thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()).setId(101L));
        assertServiceException(() -> issueService.updateProductIssue(buildSaveReq().setId(100L)),
                WM_PRODUCT_ISSUE_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.updateProductIssue(buildSaveReq().setId(100L)),
                WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testUpdateProductIssue_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> issueService.updateProductIssue(buildSaveReq().setId(100L)),
                WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

    // ==================== deleteProductIssue ====================

    @Test
    public void testDeleteProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        issueService.deleteProductIssue(100L);
        verify(issueDetailService).deleteProductIssueDetailByIssueId(100L);
        verify(issueLineService).deleteProductIssueLineByIssueId(100L);
        verify(issueMapper).deleteById(100L);
    }

    @Test
    public void testDeleteProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.deleteProductIssue(100L), WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testDeleteProductIssue_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> issueService.deleteProductIssue(100L), WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

    // ==================== get / page ====================

    @Test
    public void testGetProductIssue() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        MesWmProductIssueDO issue = issueService.getProductIssue(100L);
        assertNotNull(issue);
        assertEquals("ISSUE001", issue.getCode());
    }

    @Test
    public void testGetProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertNull(issueService.getProductIssue(100L));
    }

    @Test
    public void testGetProductIssuePage() {
        PageResult<MesWmProductIssueDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(issueMapper.selectPage(any(MesWmProductIssuePageReqVO.class))).thenReturn(page);
        assertEquals(0, issueService.getProductIssuePage(new MesWmProductIssuePageReqVO()).getTotal());
    }

    // ==================== submitProductIssue ====================

    @Test
    public void testSubmitProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        issueService.submitProductIssue(100L);
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testSubmitProductIssue_noLine() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> issueService.submitProductIssue(100L), WM_PRODUCT_ISSUE_NO_LINE);
    }

    @Test
    public void testSubmitProductIssue_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> issueService.submitProductIssue(100L), WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

    // ==================== stockProductIssue ====================

    @Test
    public void testStockProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("4")), buildDetail(new BigDecimal("6"))));
        issueService.stockProductIssue(100L);
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testStockProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.stockProductIssue(100L), WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testStockProductIssue_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> issueService.stockProductIssue(100L), WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

    @Test
    public void testStockProductIssue_quantityMismatch() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("3"))));
        assertServiceException(() -> issueService.stockProductIssue(100L),
                WM_PRODUCT_ISSUE_DETAIL_QUANTITY_MISMATCH);
    }

    // ==================== finishProductIssue ====================

    @Test
    public void testFinishProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10"))));
        when(issueDetailService.getProductIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10"))));
        issueService.finishProductIssue(100L);
        // 一条明细 -> 一笔出库 + 一笔入库
        verify(wmTransactionService, times(2)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testFinishProductIssue_multiDetail() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("4")), buildDetail(new BigDecimal("6"))));
        when(issueDetailService.getProductIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("4")), buildDetail(new BigDecimal("6"))));
        issueService.finishProductIssue(100L);
        verify(wmTransactionService, times(4)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(warehouseService).getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE);
        verify(locationService).getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION);
        verify(areaService).getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);
    }

    @Test
    public void testFinishProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.finishProductIssue(100L), WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testFinishProductIssue_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> issueService.finishProductIssue(100L), WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

    @Test
    public void testFinishProductIssue_noDetail() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
        when(issueDetailService.getProductIssueDetailListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> issueService.finishProductIssue(100L), WM_PRODUCT_ISSUE_NO_DETAIL);
    }

    @Test
    public void testFinishProductIssue_quantityMismatch() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
        when(issueDetailService.getProductIssueDetailListByIssueId(100L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("3"))));
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("3"))));
        assertServiceException(() -> issueService.finishProductIssue(100L),
                WM_PRODUCT_ISSUE_DETAIL_QUANTITY_MISMATCH);
    }

    // ==================== cancelProductIssue ====================

    @Test
    public void testCancelProductIssue_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        issueService.cancelProductIssue(100L);
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testCancelProductIssue_approving() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
        issueService.cancelProductIssue(100L);
        verify(issueMapper).updateById(any(MesWmProductIssueDO.class));
    }

    @Test
    public void testCancelProductIssue_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.cancelProductIssue(100L), WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testCancelProductIssue_finished() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> issueService.cancelProductIssue(100L), WM_PRODUCT_ISSUE_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelProductIssue_canceled() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> issueService.cancelProductIssue(100L), WM_PRODUCT_ISSUE_CANCEL_NOT_ALLOWED);
    }

    // ==================== checkProductIssueQuantity ====================

    @Test
    public void testCheckProductIssueQuantity_noLine() {
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Collections.emptyList());
        assertTrue(issueService.checkProductIssueQuantity(100L));
    }

    @Test
    public void testCheckProductIssueQuantity_match() {
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10.00"))));
        assertTrue(issueService.checkProductIssueQuantity(100L));
    }

    @Test
    public void testCheckProductIssueQuantity_mismatch() {
        when(issueLineService.getProductIssueLineListByIssueId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(issueDetailService.getProductIssueDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("9"))));
        assertFalse(issueService.checkProductIssueQuantity(100L));
    }

    @Test
    public void testCheckProductIssueQuantity_lineQuantityNull() {
        when(issueLineService.getProductIssueLineListByIssueId(100L))
                .thenReturn(Arrays.asList(buildLine().setQuantity(null)));
        when(issueDetailService.getProductIssueDetailListByLineId(200L)).thenReturn(Collections.emptyList());
        assertTrue(issueService.checkProductIssueQuantity(100L));
    }

    // ==================== validate ====================

    @Test
    public void testValidateProductIssueExists_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        MesWmProductIssueDO issue = issueService.validateProductIssueExists(100L);
        assertNotNull(issue);
        assertEquals(100L, issue.getId());
    }

    @Test
    public void testValidateProductIssueExists_notExists() {
        when(issueMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> issueService.validateProductIssueExists(100L), WM_PRODUCT_ISSUE_NOT_EXISTS);
    }

    @Test
    public void testValidateProductIssueExistsAndPrepare_success() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.PREPARE.getStatus()));
        assertNotNull(issueService.validateProductIssueExistsAndPrepare(100L));
    }

    @Test
    public void testValidateProductIssueExistsAndPrepare_statusInvalid() {
        when(issueMapper.selectById(100L)).thenReturn(buildIssue(MesWmProductIssueStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> issueService.validateProductIssueExistsAndPrepare(100L),
                WM_PRODUCT_ISSUE_STATUS_INVALID);
    }

}
