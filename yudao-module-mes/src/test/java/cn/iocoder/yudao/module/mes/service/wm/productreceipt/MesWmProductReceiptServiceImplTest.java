package cn.iocoder.yudao.module.mes.service.wm.productreceipt;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.productreceipt.vo.MesWmProductReceiptPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.productreceipt.vo.MesWmProductReceiptSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productreceipt.MesWmProductReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productreceipt.MesWmProductReceiptDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productreceipt.MesWmProductReceiptLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productreceipt.MesWmProductReceiptMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductReceiptStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * {@link MesWmProductReceiptServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmProductReceiptServiceImpl.class)
public class MesWmProductReceiptServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmProductReceiptMapper productReceiptMapper;
    @MockitoBean
    private MesWmProductReceiptLineService productReceiptLineService;
    @MockitoBean
    private MesWmProductReceiptDetailService productReceiptDetailService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesWmWarehouseService warehouseService;
    @MockitoBean
    private MesWmWarehouseLocationService locationService;
    @MockitoBean
    private MesWmWarehouseAreaService areaService;

    @Resource
    private MesWmProductReceiptServiceImpl productReceiptService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmProductReceiptDO receipt = inv.getArgument(0);
            receipt.setId(receipt.getId() == null ? 100L : receipt.getId());
            return 1;
        }).when(productReceiptMapper).insert(any(MesWmProductReceiptDO.class));
        when(productReceiptMapper.updateById(any(MesWmProductReceiptDO.class))).thenReturn(1);
        when(productReceiptMapper.deleteById(anyLong())).thenReturn(1);
        // 虚拟线边库
        when(warehouseService.getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(new MesWmWarehouseDO().setId(9001L));
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(new MesWmWarehouseLocationDO().setId(9002L));
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(new MesWmWarehouseAreaDO().setId(9003L));
        // 库存事务
        when(wmTransactionService.createTransaction(any(MesWmTransactionSaveReqDTO.class))).thenReturn(8001L);
    }

    private MesWmProductReceiptDO buildReceipt(Integer status) {
        return new MesWmProductReceiptDO().setId(100L).setCode("PR001").setName("产品收货单A")
                .setWorkOrderId(200L).setItemId(300L).setReceiptDate(LocalDateTime.now()).setStatus(status);
    }

    private MesWmProductReceiptSaveReqVO buildSaveReq() {
        MesWmProductReceiptSaveReqVO reqVO = new MesWmProductReceiptSaveReqVO();
        reqVO.setCode("PR001");
        reqVO.setName("产品收货单A");
        reqVO.setReceiptDate(LocalDateTime.now());
        reqVO.setRemark("备注");
        return reqVO;
    }

    private MesWmProductReceiptLineDO buildLine(Long id, BigDecimal quantity) {
        return new MesWmProductReceiptLineDO().setId(id).setReceiptId(100L).setItemId(300L).setQuantity(quantity);
    }

    private MesWmProductReceiptDetailDO buildDetail(Long id, Long lineId, BigDecimal quantity) {
        return new MesWmProductReceiptDetailDO().setId(id).setLineId(lineId).setReceiptId(100L)
                .setItemId(300L).setQuantity(quantity).setBatchId(400L)
                .setWarehouseId(500L).setLocationId(600L).setAreaId(700L);
    }

    // ========== createProductReceipt ==========

    @Test
    public void testCreateProductReceipt_success() {
        Long id = productReceiptService.createProductReceipt(buildSaveReq());
        assertEquals(100L, id);
        verify(productReceiptMapper).insert(any(MesWmProductReceiptDO.class));
        verify(workOrderService, never()).validateWorkOrderConfirmed(anyLong());
    }

    @Test
    public void testCreateProductReceipt_withWorkOrder() {
        when(workOrderService.validateWorkOrderConfirmed(200L))
                .thenReturn(new MesProWorkOrderDO().setId(200L).setProductId(300L));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setWorkOrderId(200L);

        Long id = productReceiptService.createProductReceipt(reqVO);
        assertEquals(100L, id);
        verify(workOrderService).validateWorkOrderConfirmed(200L);
        verify(productReceiptMapper).insert(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testCreateProductReceipt_codeDuplicate() {
        when(productReceiptMapper.selectByCode("PR001"))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> productReceiptService.createProductReceipt(buildSaveReq()),
                WM_PRODUCT_RECPT_CODE_DUPLICATE);
    }

    // ========== updateProductReceipt ==========

    @Test
    public void testUpdateProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setName("产品收货单B");

        productReceiptService.updateProductReceipt(reqVO);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testUpdateProductReceipt_withWorkOrder() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        when(workOrderService.validateWorkOrderConfirmed(200L))
                .thenReturn(new MesProWorkOrderDO().setId(200L).setProductId(300L));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        reqVO.setWorkOrderId(200L);

        productReceiptService.updateProductReceipt(reqVO);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testUpdateProductReceipt_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> productReceiptService.updateProductReceipt(reqVO), WM_PRODUCT_RECPT_NOT_EXISTS);
    }

    @Test
    public void testUpdateProductReceipt_statusNotPrepare() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVING.getStatus()));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> productReceiptService.updateProductReceipt(reqVO),
                WM_PRODUCT_RECPT_STATUS_NOT_PREPARE);
    }

    @Test
    public void testUpdateProductReceipt_codeDuplicate() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        when(productReceiptMapper.selectByCode("PR001"))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()).setId(101L));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> productReceiptService.updateProductReceipt(reqVO),
                WM_PRODUCT_RECPT_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateProductReceipt_sameCodeSameId() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        when(productReceiptMapper.selectByCode("PR001"))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        MesWmProductReceiptSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);

        productReceiptService.updateProductReceipt(reqVO);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    // ========== deleteProductReceipt ==========

    @Test
    public void testDeleteProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));

        productReceiptService.deleteProductReceipt(100L);
        verify(productReceiptDetailService).deleteProductReceiptDetailByRecptId(100L);
        verify(productReceiptLineService).deleteProductReceiptLineByRecptId(100L);
        verify(productReceiptMapper).deleteById(100L);
    }

    @Test
    public void testDeleteProductReceipt_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> productReceiptService.deleteProductReceipt(100L), WM_PRODUCT_RECPT_NOT_EXISTS);
    }

    @Test
    public void testDeleteProductReceipt_statusNotPrepare() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> productReceiptService.deleteProductReceipt(100L),
                WM_PRODUCT_RECPT_STATUS_NOT_PREPARE);
    }

    // ========== get / page ==========

    @Test
    public void testGetProductReceipt() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        assertNotNull(productReceiptService.getProductReceipt(100L));
    }

    @Test
    public void testGetProductReceipt_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        assertNull(productReceiptService.getProductReceipt(100L));
    }

    @Test
    public void testGetProductReceiptPage() {
        PageResult<MesWmProductReceiptDO> page = new PageResult<>(
                Arrays.asList(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus())), 1L);
        when(productReceiptMapper.selectPage(any(MesWmProductReceiptPageReqVO.class))).thenReturn(page);

        PageResult<MesWmProductReceiptDO> result = productReceiptService.getProductReceiptPage(
                new MesWmProductReceiptPageReqVO());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    // ========== submitProductReceipt ==========

    @Test
    public void testSubmitProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));

        productReceiptService.submitProductReceipt(100L);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testSubmitProductReceipt_noLine() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L))
                .thenReturn(Collections.emptyList());
        assertServiceException(() -> productReceiptService.submitProductReceipt(100L), WM_PRODUCT_RECPT_NO_LINE);
    }

    @Test
    public void testSubmitProductReceipt_statusNotPrepare() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> productReceiptService.submitProductReceipt(100L),
                WM_PRODUCT_RECPT_STATUS_NOT_PREPARE);
    }

    // ========== stockProductReceipt ==========

    @Test
    public void testStockProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVING.getStatus()));

        productReceiptService.stockProductReceipt(100L);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testStockProductReceipt_statusError() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> productReceiptService.stockProductReceipt(100L), WM_PRODUCT_RECPT_STATUS_ERROR);
    }

    @Test
    public void testStockProductReceipt_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> productReceiptService.stockProductReceipt(100L), WM_PRODUCT_RECPT_NOT_EXISTS);
    }

    // ========== checkProductReceiptQuantity ==========

    @Test
    public void testCheckProductReceiptQuantity_noLine() {
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L)).thenReturn(Collections.emptyList());
        assertTrue(productReceiptService.checkProductReceiptQuantity(100L));
    }

    @Test
    public void testCheckProductReceiptQuantity_match() {
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(productReceiptDetailService.getProductReceiptDetailListByLineId(1L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4")),
                        buildDetail(12L, 1L, new BigDecimal("6"))));
        assertTrue(productReceiptService.checkProductReceiptQuantity(100L));
    }

    @Test
    public void testCheckProductReceiptQuantity_mismatch() {
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, new BigDecimal("10"))));
        when(productReceiptDetailService.getProductReceiptDetailListByLineId(1L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("4"))));
        assertFalse(productReceiptService.checkProductReceiptQuantity(100L));
    }

    @Test
    public void testCheckProductReceiptQuantity_lineQuantityNull() {
        when(productReceiptLineService.getProductReceiptLineListByRecptId(100L))
                .thenReturn(Arrays.asList(buildLine(1L, null)));
        assertTrue(productReceiptService.checkProductReceiptQuantity(100L));
    }

    // ========== finishProductReceipt ==========

    @Test
    public void testFinishProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVED.getStatus()));
        when(productReceiptDetailService.getProductReceiptDetailListByRecptId(100L))
                .thenReturn(Arrays.asList(buildDetail(11L, 1L, new BigDecimal("10"))));

        productReceiptService.finishProductReceipt(100L);
        // 每条明细产生 OUT + IN 两条事务
        verify(wmTransactionService, times(2)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testFinishProductReceipt_statusError() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> productReceiptService.finishProductReceipt(100L), WM_PRODUCT_RECPT_STATUS_ERROR);
    }

    @Test
    public void testFinishProductReceipt_noDetail() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVED.getStatus()));
        when(productReceiptDetailService.getProductReceiptDetailListByRecptId(100L))
                .thenReturn(Collections.emptyList());
        assertServiceException(() -> productReceiptService.finishProductReceipt(100L), WM_PRODUCT_RECPT_NO_DETAIL);
    }

    // ========== cancelProductReceipt ==========

    @Test
    public void testCancelProductReceipt_success() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));

        productReceiptService.cancelProductReceipt(100L);
        verify(productReceiptMapper).updateById(any(MesWmProductReceiptDO.class));
    }

    @Test
    public void testCancelProductReceipt_finishedNotAllowed() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> productReceiptService.cancelProductReceipt(100L),
                WM_PRODUCT_RECPT_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelProductReceipt_canceledNotAllowed() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> productReceiptService.cancelProductReceipt(100L),
                WM_PRODUCT_RECPT_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelProductReceipt_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> productReceiptService.cancelProductReceipt(100L), WM_PRODUCT_RECPT_NOT_EXISTS);
    }

    // ========== validateProductReceiptEditable ==========

    @Test
    public void testValidateProductReceiptEditable_prepare() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.PREPARE.getStatus()));
        assertNotNull(productReceiptService.validateProductReceiptEditable(100L));
    }

    @Test
    public void testValidateProductReceiptEditable_approving() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVING.getStatus()));
        assertNotNull(productReceiptService.validateProductReceiptEditable(100L));
    }

    @Test
    public void testValidateProductReceiptEditable_statusNotAllowed() {
        when(productReceiptMapper.selectById(100L))
                .thenReturn(buildReceipt(MesWmProductReceiptStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> productReceiptService.validateProductReceiptEditable(100L),
                WM_PRODUCT_RECPT_STATUS_NOT_PREPARE);
    }

    @Test
    public void testValidateProductReceiptEditable_notExists() {
        when(productReceiptMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> productReceiptService.validateProductReceiptEditable(100L),
                WM_PRODUCT_RECPT_NOT_EXISTS);
    }

}
