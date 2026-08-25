package cn.zhicloud.module.mes.service.wm.productsales;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.productsales.vo.detail.MesWmProductSalesDetailSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productsales.MesWmProductSalesDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productsales.MesWmProductSalesLineDO;
import cn.zhicloud.module.mes.dal.mysql.wm.productsales.MesWmProductSalesDetailMapper;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import cn.zhicloud.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import cn.zhicloud.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Arrays;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmProductSalesDetailServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmProductSalesDetailServiceImpl.class)
public class MesWmProductSalesDetailServiceImplTest extends BaseDbUnitTest {

    private static final Long ID = 500L;
    private static final Long LINE_ID = 200L;
    private static final Long SALES_ID = 100L;
    private static final Long ITEM_ID = 1L;

    @MockitoBean
    private MesWmProductSalesDetailMapper productSalesDetailMapper;
    @MockitoBean
    private MesMdItemService itemService;
    @MockitoBean
    private MesWmProductSalesLineService productSalesLineService;
    @MockitoBean
    private MesWmMaterialStockService materialStockService;
    @MockitoBean
    private MesWmWarehouseAreaService warehouseAreaService;

    @Resource
    private MesWmProductSalesDetailServiceImpl productSalesDetailService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmProductSalesDetailDO detail = inv.getArgument(0);
            detail.setId(detail.getId() == null ? ID : detail.getId());
            return 1;
        }).when(productSalesDetailMapper).insert(any(MesWmProductSalesDetailDO.class));
        when(productSalesDetailMapper.updateById(any(MesWmProductSalesDetailDO.class))).thenReturn(1);
        when(productSalesDetailMapper.deleteById(anyLong())).thenReturn(1);
        // 默认行存在且匹配
        when(productSalesLineService.getProductSalesLine(LINE_ID)).thenReturn(buildLine(SALES_ID, ITEM_ID));
    }

    private MesWmProductSalesLineDO buildLine(Long salesId, Long itemId) {
        return new MesWmProductSalesLineDO().setId(LINE_ID).setSalesId(salesId).setItemId(itemId)
                .setQuantity(new BigDecimal("10"));
    }

    private MesWmProductSalesDetailDO buildDetail() {
        return new MesWmProductSalesDetailDO().setId(ID).setLineId(LINE_ID).setSalesId(SALES_ID)
                .setItemId(ITEM_ID).setQuantity(new BigDecimal("10"))
                .setWarehouseId(3L).setLocationId(4L).setAreaId(5L);
    }

    private MesWmProductSalesDetailSaveReqVO buildSaveReq() {
        MesWmProductSalesDetailSaveReqVO reqVO = new MesWmProductSalesDetailSaveReqVO();
        reqVO.setLineId(LINE_ID);
        reqVO.setSalesId(SALES_ID);
        reqVO.setItemId(ITEM_ID);
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setWarehouseId(3L);
        reqVO.setLocationId(4L);
        reqVO.setAreaId(5L);
        return reqVO;
    }

    // ========== createProductSalesDetail ==========

    @Test
    public void testCreateProductSalesDetail_success() {
        Long id = productSalesDetailService.createProductSalesDetail(buildSaveReq());
        assertEquals(ID, id);
        verify(productSalesDetailMapper).insert(any(MesWmProductSalesDetailDO.class));
        verify(itemService).validateItemExistsAndEnable(ITEM_ID);
        verify(warehouseAreaService).validateWarehouseAreaExists(3L, 4L, 5L);
    }

    @Test
    public void testCreateProductSalesDetail_lineNotExists() {
        when(productSalesLineService.getProductSalesLine(LINE_ID)).thenReturn(null);
        assertServiceException(() -> productSalesDetailService.createProductSalesDetail(buildSaveReq()),
                WM_PRODUCT_SALES_LINE_NOT_EXISTS);
    }

    @Test
    public void testCreateProductSalesDetail_lineNotMatch() {
        when(productSalesLineService.getProductSalesLine(LINE_ID)).thenReturn(buildLine(999L, ITEM_ID));
        assertServiceException(() -> productSalesDetailService.createProductSalesDetail(buildSaveReq()),
                WM_PRODUCT_SALES_DETAIL_LINE_NOT_MATCH);
    }

    @Test
    public void testCreateProductSalesDetail_itemMismatch() {
        when(productSalesLineService.getProductSalesLine(LINE_ID)).thenReturn(buildLine(SALES_ID, 999L));
        assertServiceException(() -> productSalesDetailService.createProductSalesDetail(buildSaveReq()),
                WM_PRODUCT_SALES_DETAIL_ITEM_MISMATCH);
    }

    // ========== updateProductSalesDetail ==========

    @Test
    public void testUpdateProductSalesDetail_success() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(buildDetail());
        MesWmProductSalesDetailSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);
        reqVO.setQuantity(new BigDecimal("8"));

        productSalesDetailService.updateProductSalesDetail(reqVO);
        verify(productSalesDetailMapper).updateById(any(MesWmProductSalesDetailDO.class));
    }

    @Test
    public void testUpdateProductSalesDetail_notExists() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(null);
        MesWmProductSalesDetailSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(ID);

        assertServiceException(() -> productSalesDetailService.updateProductSalesDetail(reqVO),
                WM_PRODUCT_SALES_DETAIL_NOT_EXISTS);
    }

    // ========== deleteProductSalesDetail ==========

    @Test
    public void testDeleteProductSalesDetail_success() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(buildDetail());
        productSalesDetailService.deleteProductSalesDetail(ID);
        verify(productSalesDetailMapper).deleteById(ID);
    }

    @Test
    public void testDeleteProductSalesDetail_notExists() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(null);
        assertServiceException(() -> productSalesDetailService.deleteProductSalesDetail(ID),
                WM_PRODUCT_SALES_DETAIL_NOT_EXISTS);
    }

    // ========== get / list / 批量删除 ==========

    @Test
    public void testGetProductSalesDetail() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(buildDetail());
        assertNotNull(productSalesDetailService.getProductSalesDetail(ID));
    }

    @Test
    public void testGetProductSalesDetail_notExists() {
        when(productSalesDetailMapper.selectById(ID)).thenReturn(null);
        assertNull(productSalesDetailService.getProductSalesDetail(ID));
    }

    @Test
    public void testGetProductSalesDetailListBySalesId() {
        when(productSalesDetailMapper.selectListBySalesId(SALES_ID)).thenReturn(Arrays.asList(buildDetail()));
        assertEquals(1, productSalesDetailService.getProductSalesDetailListBySalesId(SALES_ID).size());
    }

    @Test
    public void testGetProductSalesDetailListByLineId() {
        when(productSalesDetailMapper.selectListByLineId(LINE_ID)).thenReturn(Arrays.asList(buildDetail()));
        assertEquals(1, productSalesDetailService.getProductSalesDetailListByLineId(LINE_ID).size());
    }

    @Test
    public void testDeleteProductSalesDetailByLineId() {
        productSalesDetailService.deleteProductSalesDetailByLineId(LINE_ID);
        verify(productSalesDetailMapper).deleteByLineId(LINE_ID);
    }

    @Test
    public void testDeleteProductSalesDetailBySalesId() {
        productSalesDetailService.deleteProductSalesDetailBySalesId(SALES_ID);
        verify(productSalesDetailMapper).deleteBySalesId(SALES_ID);
    }

}
