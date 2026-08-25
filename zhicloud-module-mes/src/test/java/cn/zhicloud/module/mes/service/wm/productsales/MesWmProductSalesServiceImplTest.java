package cn.zhicloud.module.mes.service.wm.productsales;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.productsales.vo.MesWmProductSalesPageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.productsales.vo.MesWmProductSalesSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.productsales.vo.MesWmProductSalesShippingReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productsales.MesWmProductSalesDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productsales.MesWmProductSalesDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productsales.MesWmProductSalesLineDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.salesnotice.MesWmSalesNoticeDO;
import cn.zhicloud.module.mes.dal.mysql.wm.productsales.MesWmProductSalesMapper;
import cn.zhicloud.module.mes.enums.wm.MesWmProductSalesStatusEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmQualityStatusEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmSalesNoticeStatusEnum;
import cn.zhicloud.module.mes.service.md.client.MesMdClientService;
import cn.zhicloud.module.mes.service.wm.salesnotice.MesWmSalesNoticeService;
import cn.zhicloud.module.mes.service.wm.transaction.MesWmTransactionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmProductSalesServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmProductSalesServiceImpl.class)
public class MesWmProductSalesServiceImplTest extends BaseDbUnitTest {

    private static final Long ID = 100L;
    private static final Long LINE_ID = 200L;
    private static final Long CLIENT_ID = 300L;
    private static final Long NOTICE_ID = 400L;

    @MockitoBean
    private MesWmProductSalesMapper productSalesMapper;
    @MockitoBean
    private MesWmProductSalesLineService productSalesLineService;
    @MockitoBean
    private MesWmProductSalesDetailService productSalesDetailService;
    @MockitoBean
    private MesMdClientService clientService;
    @MockitoBean
    private MesWmSalesNoticeService salesNoticeService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;

    @Resource
    private MesWmProductSalesServiceImpl productSalesService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmProductSalesDO sales = inv.getArgument(0);
            sales.setId(sales.getId() == null ? ID : sales.getId());
            return 1;
        }).when(productSalesMapper).insert(any(MesWmProductSalesDO.class));
        when(productSalesMapper.updateById(any(MesWmProductSalesDO.class))).thenReturn(1);
        when(productSalesMapper.deleteById(anyLong())).thenReturn(1);
    }

    private MesWmProductSalesDO buildSales(Integer status) {
        return new MesWmProductSalesDO().setId(ID).setCode("PS001").setName("产品出库单")
                .setClientId(CLIENT_ID).setSalesDate(LocalDateTime.now()).setStatus(status);
    }

    private MesWmProductSalesSaveReqVO buildSaveReq() {
        MesWmProductSalesSaveReqVO reqVO = new MesWmProductSalesSaveReqVO();
        reqVO.setCode("PS001");
        reqVO.setName("产品出库单");
        reqVO.setClientId(CLIENT_ID);
        reqVO.setSalesDate(LocalDateTime.now());
        return reqVO;
    }

    private MesWmProductSalesLineDO buildLine(BigDecimal quantity, Boolean oqcCheckFlag) {
        return new MesWmProductSalesLineDO().setId(LINE_ID).setSalesId(ID).setItemId(1L)
                .setQuantity(quantity).setOqcCheckFlag(oqcCheckFlag);
    }

    private MesWmProductSalesDetailDO buildDetail(BigDecimal quantity) {
        return new MesWmProductSalesDetailDO().setId(500L).setLineId(LINE_ID).setSalesId(ID)
                .setItemId(1L).setQuantity(quantity).setBatchId(2L).setBatchCode("B001")
                .setWarehouseId(3L).setLocationId(4L).setAreaId(5L);
    }

    private MesWmProductSalesDO captureUpdate() {
        ArgumentCaptor<MesWmProductSalesDO> captor = ArgumentCaptor.forClass(MesWmProductSalesDO.class);
        verify(productSalesMapper).updateById(captor.capture());
        return captor.getValue();
    }

    // ========== createProductSales ==========

    @Test
    public void testCreateProductSales_success() {
        Long id = productSalesService.createProductSales(buildSaveReq());
        assertEquals(ID, id);
        verify(productSalesMapper).insert(any(MesWmProductSalesDO.class));
        verify(clientService).validateClientExistsAndEnable(CLIENT_ID);
    }

    @Test
    public void testCreateProductSales_codeDuplicate() {
        when(productSalesMapper.selectByCode("PS001")).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> productSalesService.createProductSales(buildSaveReq()),
                WM_PRODUCT_SALES_CODE_DUPLICATE);
    }

    @Test
    public void testCreateProductSales_withNotice_success() {
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setNoticeId(NOTICE_ID);
        when(salesNoticeService.validateSalesNoticeExists(NOTICE_ID)).thenReturn(new MesWmSalesNoticeDO()
                .setId(NOTICE_ID).setClientId(CLIENT_ID)
                .setStatus(MesWmSalesNoticeStatusEnum.APPROVED.getStatus()));

        assertEquals(ID, productSalesService.createProductSales(reqVO));
        verify(productSalesMapper).insert(any(MesWmProductSalesDO.class));
    }

    @Test
    public void testCreateProductSales_notice_statusNotApproved() {
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setNoticeId(NOTICE_ID);
        when(salesNoticeService.validateSalesNoticeExists(NOTICE_ID)).thenReturn(new MesWmSalesNoticeDO()
                .setId(NOTICE_ID).setClientId(CLIENT_ID)
                .setStatus(MesWmSalesNoticeStatusEnum.PREPARE.getStatus()));

        assertServiceException(() -> productSalesService.createProductSales(reqVO),
                WM_SALES_NOTICE_STATUS_NOT_APPROVED);
    }

    @Test
    public void testCreateProductSales_notice_clientMismatch() {
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setNoticeId(NOTICE_ID);
        when(salesNoticeService.validateSalesNoticeExists(NOTICE_ID)).thenReturn(new MesWmSalesNoticeDO()
                .setId(NOTICE_ID).setClientId(999L)
                .setStatus(MesWmSalesNoticeStatusEnum.APPROVED.getStatus()));

        assertServiceException(() -> productSalesService.createProductSales(reqVO),
                WM_SALES_NOTICE_CLIENT_MISMATCH);
    }

    // ========== updateProductSales ==========

    @Test
    public void testUpdateProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);
        reqVO.setName("产品出库单B");

        productSalesService.updateProductSales(reqVO);
        verify(productSalesMapper).updateById(any(MesWmProductSalesDO.class));
    }

    @Test
    public void testUpdateProductSales_codeDuplicate() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesMapper.selectByCode("PS001")).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()).setId(999L));
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);

        assertServiceException(() -> productSalesService.updateProductSales(reqVO),
                WM_PRODUCT_SALES_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateProductSales_notExists() {
        when(productSalesMapper.selectById(ID)).thenReturn(null);
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);

        assertServiceException(() -> productSalesService.updateProductSales(reqVO),
                WM_PRODUCT_SALES_NOT_EXISTS);
    }

    @Test
    public void testUpdateProductSales_notPrepare() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.FINISHED.getStatus()));
        MesWmProductSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);

        assertServiceException(() -> productSalesService.updateProductSales(reqVO),
                WM_PRODUCT_SALES_NOT_PREPARE);
    }

    // ========== deleteProductSales ==========

    @Test
    public void testDeleteProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));

        productSalesService.deleteProductSales(ID);
        verify(productSalesDetailService).deleteProductSalesDetailBySalesId(ID);
        verify(productSalesLineService).deleteProductSalesLineBySalesId(ID);
        verify(productSalesMapper).deleteById(ID);
    }

    @Test
    public void testDeleteProductSales_notExists() {
        when(productSalesMapper.selectById(ID)).thenReturn(null);
        assertServiceException(() -> productSalesService.deleteProductSales(ID),
                WM_PRODUCT_SALES_NOT_EXISTS);
    }

    @Test
    public void testDeleteProductSales_notPrepare() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> productSalesService.deleteProductSales(ID),
                WM_PRODUCT_SALES_NOT_PREPARE);
    }

    // ========== get / page / list ==========

    @Test
    public void testGetProductSales() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        assertNotNull(productSalesService.getProductSales(ID));
    }

    @Test
    public void testGetProductSales_notExists() {
        when(productSalesMapper.selectById(ID)).thenReturn(null);
        assertNull(productSalesService.getProductSales(ID));
    }

    @Test
    public void testGetProductSalesPage() {
        PageResult<MesWmProductSalesDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(productSalesMapper.selectPage(any(MesWmProductSalesPageReqVO.class))).thenReturn(page);
        assertEquals(0, productSalesService.getProductSalesPage(new MesWmProductSalesPageReqVO()).getTotal());
    }

    @Test
    public void testGetProductSalesListByClientId() {
        when(productSalesMapper.selectListByClientId(CLIENT_ID)).thenReturn(
                Arrays.asList(buildSales(MesWmProductSalesStatusEnum.PREPARE.getStatus())));
        assertEquals(1, productSalesService.getProductSalesListByClientId(CLIENT_ID).size());
    }

    // ========== validateProductSalesExistsAndDraft ==========

    @Test
    public void testValidateProductSalesExistsAndDraft_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        assertNotNull(productSalesService.validateProductSalesExistsAndDraft(ID));
    }

    @Test
    public void testValidateProductSalesExistsAndDraft_notPrepare() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> productSalesService.validateProductSalesExistsAndDraft(ID),
                WM_PRODUCT_SALES_NOT_PREPARE);
    }

    // ========== submitProductSales ==========

    @Test
    public void testSubmitProductSales_linesEmpty() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID)).thenReturn(Collections.emptyList());

        assertServiceException(() -> productSalesService.submitProductSales(ID), WM_PRODUCT_SALES_LINES_EMPTY);
    }

    @Test
    public void testSubmitProductSales_quantityNull() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(null, false)));

        assertServiceException(() -> productSalesService.submitProductSales(ID),
                WM_PRODUCT_SALES_LINE_QUANTITY_INVALID);
    }

    @Test
    public void testSubmitProductSales_quantityZero() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(BigDecimal.ZERO, false)));

        assertServiceException(() -> productSalesService.submitProductSales(ID),
                WM_PRODUCT_SALES_LINE_QUANTITY_INVALID);
    }

    @Test
    public void testSubmitProductSales_needOqc() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), true)));

        productSalesService.submitProductSales(ID);

        verify(productSalesLineService).updateProductSalesLineQualityStatus(
                anyList(), eq(MesWmQualityStatusEnum.PENDING.getStatus()));
        assertEquals(MesWmProductSalesStatusEnum.CONFIRMED.getStatus(), captureUpdate().getStatus());
    }

    @Test
    public void testSubmitProductSales_noOqc() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));

        productSalesService.submitProductSales(ID);

        verify(productSalesLineService, never()).updateProductSalesLineQualityStatus(anyList(), anyInt());
        assertEquals(MesWmProductSalesStatusEnum.APPROVING.getStatus(), captureUpdate().getStatus());
    }

    // ========== checkProductSalesQuantity ==========

    @Test
    public void testCheckProductSalesQuantity_linesEmpty() {
        when(productSalesLineService.getProductSalesLineListBySalesId(ID)).thenReturn(Collections.emptyList());
        assertTrue(productSalesService.checkProductSalesQuantity(ID));
    }

    @Test
    public void testCheckProductSalesQuantity_detailsEmpty() {
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));
        when(productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID))
                .thenReturn(Collections.emptyList());

        assertFalse(productSalesService.checkProductSalesQuantity(ID));
    }

    @Test
    public void testCheckProductSalesQuantity_mismatch() {
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));
        when(productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("4"))));

        assertFalse(productSalesService.checkProductSalesQuantity(ID));
    }

    @Test
    public void testCheckProductSalesQuantity_match() {
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));
        when(productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("6")), buildDetail(new BigDecimal("4"))));

        assertTrue(productSalesService.checkProductSalesQuantity(ID));
    }

    // ========== stockProductSales ==========

    @Test
    public void testStockProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.APPROVING.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));
        when(productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10"))));

        productSalesService.stockProductSales(ID);
        assertEquals(MesWmProductSalesStatusEnum.SHIPPING.getStatus(), captureUpdate().getStatus());
    }

    @Test
    public void testStockProductSales_notExists() {
        when(productSalesMapper.selectById(ID)).thenReturn(null);
        assertServiceException(() -> productSalesService.stockProductSales(ID), WM_PRODUCT_SALES_NOT_EXISTS);
    }

    @Test
    public void testStockProductSales_cannotPick() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> productSalesService.stockProductSales(ID), WM_PRODUCT_SALES_CANNOT_PICK);
    }

    @Test
    public void testStockProductSales_detailsEmpty() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.APPROVING.getStatus()));
        when(productSalesLineService.getProductSalesLineListBySalesId(ID))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"), false)));
        when(productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID))
                .thenReturn(Collections.emptyList());

        assertServiceException(() -> productSalesService.stockProductSales(ID), WM_PRODUCT_SALES_DETAILS_EMPTY);
    }

    // ========== shippingProductSales ==========

    @Test
    public void testShippingProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.SHIPPING.getStatus()));
        MesWmProductSalesShippingReqVO reqVO = new MesWmProductSalesShippingReqVO();
        reqVO.setId(ID);
        reqVO.setCarrier("顺丰快递");
        reqVO.setShippingNumber("SF123");

        productSalesService.shippingProductSales(reqVO);

        MesWmProductSalesDO updateObj = captureUpdate();
        assertEquals(MesWmProductSalesStatusEnum.APPROVED.getStatus(), updateObj.getStatus());
        assertEquals("顺丰快递", updateObj.getCarrier());
        assertEquals("SF123", updateObj.getShippingNumber());
    }

    @Test
    public void testShippingProductSales_cannotShipping() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        MesWmProductSalesShippingReqVO reqVO = new MesWmProductSalesShippingReqVO();
        reqVO.setId(ID);

        assertServiceException(() -> productSalesService.shippingProductSales(reqVO),
                WM_PRODUCT_SALES_CANNOT_SHIPPING);
    }

    // ========== finishProductSales ==========

    @Test
    public void testFinishProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.APPROVED.getStatus()));
        when(productSalesDetailService.getProductSalesDetailListBySalesId(ID))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10"))));

        productSalesService.finishProductSales(ID);

        verify(wmTransactionService).createTransactionList(anyList());
        assertEquals(MesWmProductSalesStatusEnum.FINISHED.getStatus(), captureUpdate().getStatus());
    }

    @Test
    public void testFinishProductSales_cannotFinish() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.SHIPPING.getStatus()));
        assertServiceException(() -> productSalesService.finishProductSales(ID), WM_PRODUCT_SALES_CANNOT_FINISH);
    }

    @Test
    public void testFinishProductSales_notExists() {
        when(productSalesMapper.selectById(ID)).thenReturn(null);
        assertServiceException(() -> productSalesService.finishProductSales(ID), WM_PRODUCT_SALES_NOT_EXISTS);
    }

    // ========== cancelProductSales ==========

    @Test
    public void testCancelProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.APPROVING.getStatus()));

        productSalesService.cancelProductSales(ID);
        assertEquals(MesWmProductSalesStatusEnum.CANCELED.getStatus(), captureUpdate().getStatus());
    }

    @Test
    public void testCancelProductSales_finished() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> productSalesService.cancelProductSales(ID), WM_PRODUCT_SALES_CANNOT_CANCEL);
    }

    @Test
    public void testCancelProductSales_canceled() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> productSalesService.cancelProductSales(ID), WM_PRODUCT_SALES_CANNOT_CANCEL);
    }

    // ========== confirmProductSales ==========

    @Test
    public void testConfirmProductSales_success() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.CONFIRMED.getStatus()));

        productSalesService.confirmProductSales(ID);
        assertEquals(MesWmProductSalesStatusEnum.APPROVING.getStatus(), captureUpdate().getStatus());
    }

    @Test
    public void testConfirmProductSales_cannotConfirm() {
        when(productSalesMapper.selectById(ID)).thenReturn(buildSales(
                MesWmProductSalesStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> productSalesService.confirmProductSales(ID), WM_PRODUCT_SALES_CANNOT_CONFIRM);
    }

}
