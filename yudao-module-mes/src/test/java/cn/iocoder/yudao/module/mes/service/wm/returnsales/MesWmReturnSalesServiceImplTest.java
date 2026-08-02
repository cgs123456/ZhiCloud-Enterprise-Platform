package cn.iocoder.yudao.module.mes.service.wm.returnsales;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.returnsales.vo.MesWmReturnSalesPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.returnsales.vo.MesWmReturnSalesSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnsales.MesWmReturnSalesDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnsales.MesWmReturnSalesDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.returnsales.MesWmReturnSalesLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.returnsales.MesWmReturnSalesMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmQualityStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmReturnSalesStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Resource;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmReturnSalesServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmReturnSalesServiceImpl.class)
public class MesWmReturnSalesServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmReturnSalesMapper returnSalesMapper;
    @MockitoBean
    private MesWmReturnSalesLineService returnSalesLineService;
    @MockitoBean
    private MesWmReturnSalesDetailService returnSalesDetailService;
    @MockitoBean
    private MesMdClientService clientService;
    @MockitoBean
    private MesMdItemService itemService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;

    @Resource
    private MesWmReturnSalesServiceImpl returnSalesService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmReturnSalesDO returnSales = inv.getArgument(0);
            returnSales.setId(returnSales.getId() == null ? 200L : returnSales.getId());
            return 1;
        }).when(returnSalesMapper).insert(any(MesWmReturnSalesDO.class));
        when(returnSalesMapper.updateById(any(MesWmReturnSalesDO.class))).thenReturn(1);
        when(returnSalesMapper.deleteById(anyLong())).thenReturn(1);
    }

    private MesWmReturnSalesDO buildReturnSales(Integer status) {
        return new MesWmReturnSalesDO().setId(200L).setCode("RS001").setName("销售退货")
                .setSalesOrderCode("SO001").setClientId(5L).setReturnReason("质量问题").setStatus(status);
    }

    private MesWmReturnSalesSaveReqVO buildSaveReq() {
        MesWmReturnSalesSaveReqVO reqVO = new MesWmReturnSalesSaveReqVO();
        reqVO.setCode("RS001");
        reqVO.setName("销售退货");
        reqVO.setSalesOrderCode("SO001");
        reqVO.setClientId(5L);
        reqVO.setReturnDate(LocalDateTime.now());
        reqVO.setReturnReason("质量问题");
        reqVO.setRemark("备注");
        return reqVO;
    }

    private MesWmReturnSalesLineDO buildLine(Long id, Integer qualityStatus, String quantity) {
        return new MesWmReturnSalesLineDO().setId(id).setReturnId(200L).setItemId(10L)
                .setQuantity(new BigDecimal(quantity)).setQualityStatus(qualityStatus);
    }

    private MesWmReturnSalesDetailDO buildDetail(Long id, Long lineId, String quantity) {
        return new MesWmReturnSalesDetailDO().setId(id).setReturnId(200L).setLineId(lineId).setItemId(10L)
                .setQuantity(new BigDecimal(quantity)).setBatchId(20L).setBatchCode("B001")
                .setWarehouseId(30L).setLocationId(31L).setAreaId(32L);
    }

    // ========== createReturnSales ==========

    @Test
    public void testCreateReturnSales_success() {
        Long id = returnSalesService.createReturnSales(buildSaveReq());
        assertEquals(200L, id);
        verify(returnSalesMapper).insert(any(MesWmReturnSalesDO.class));
        verify(clientService).validateClientExistsAndEnable(5L);
    }

    @Test
    public void testCreateReturnSales_codeDuplicate() {
        when(returnSalesMapper.selectByCode("RS001"))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> returnSalesService.createReturnSales(buildSaveReq()), WM_RETURN_SALES_CODE_DUPLICATE);
    }

    // ========== updateReturnSales ==========

    @Test
    public void testUpdateReturnSales_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        MesWmReturnSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(200L);
        reqVO.setName("销售退货B");
        returnSalesService.updateReturnSales(reqVO);
        verify(returnSalesMapper).updateById(any(MesWmReturnSalesDO.class));
    }

    @Test
    public void testUpdateReturnSales_sameCodeAllowed() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        when(returnSalesMapper.selectByCode("RS001"))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        MesWmReturnSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(200L);
        returnSalesService.updateReturnSales(reqVO);
        verify(returnSalesMapper).updateById(any(MesWmReturnSalesDO.class));
    }

    @Test
    public void testUpdateReturnSales_notExists() {
        when(returnSalesMapper.selectById(200L)).thenReturn(null);
        MesWmReturnSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(200L);
        assertServiceException(() -> returnSalesService.updateReturnSales(reqVO), WM_RETURN_SALES_NOT_EXISTS);
    }

    @Test
    public void testUpdateReturnSales_notPrepare() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.FINISHED.getStatus()));
        MesWmReturnSalesSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(200L);
        assertServiceException(() -> returnSalesService.updateReturnSales(reqVO), WM_RETURN_SALES_STATUS_NOT_PREPARE);
    }

    // ========== deleteReturnSales ==========

    @Test
    public void testDeleteReturnSales_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        returnSalesService.deleteReturnSales(200L);
        verify(returnSalesDetailService).deleteReturnSalesDetailByReturnId(200L);
        verify(returnSalesLineService).deleteReturnSalesLineByReturnId(200L);
        verify(returnSalesMapper).deleteById(200L);
    }

    @Test
    public void testDeleteReturnSales_notExists() {
        when(returnSalesMapper.selectById(200L)).thenReturn(null);
        assertServiceException(() -> returnSalesService.deleteReturnSales(200L), WM_RETURN_SALES_NOT_EXISTS);
    }

    @Test
    public void testDeleteReturnSales_notPrepare() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> returnSalesService.deleteReturnSales(200L), WM_RETURN_SALES_STATUS_NOT_PREPARE);
    }

    // ========== get / page ==========

    @Test
    public void testGetReturnSales() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnSalesService.getReturnSales(200L));
    }

    @Test
    public void testGetReturnSales_null() {
        when(returnSalesMapper.selectById(200L)).thenReturn(null);
        assertNull(returnSalesService.getReturnSales(200L));
    }

    @Test
    public void testGetReturnSalesPage() {
        PageResult<MesWmReturnSalesDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(returnSalesMapper.selectPage(any(MesWmReturnSalesPageReqVO.class))).thenReturn(page);
        assertEquals(0, returnSalesService.getReturnSalesPage(new MesWmReturnSalesPageReqVO()).getTotal());
    }

    // ========== submitReturnSales ==========

    @Test
    public void testSubmitReturnSales_noLine() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> returnSalesService.submitReturnSales(200L), WM_RETURN_SALES_NO_LINE);
    }

    @Test
    public void testSubmitReturnSales_hasPendingQc() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Arrays.asList(
                buildLine(1L, MesWmQualityStatusEnum.PENDING.getStatus(), "10"),
                buildLine(2L, MesWmQualityStatusEnum.PASS.getStatus(), "5")));
        returnSalesService.submitReturnSales(200L);
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.CONFIRMED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testSubmitReturnSales_noPendingQc() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        returnSalesService.submitReturnSales(200L);
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.APPROVING.getStatus(), captor.getValue().getStatus());
    }

    // ========== finishReturnSales ==========

    @Test
    public void testFinishReturnSales_notApproving() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> returnSalesService.finishReturnSales(200L), WM_RETURN_SALES_STATUS_NOT_APPROVING);
    }

    @Test
    public void testFinishReturnSales_noLine() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVING.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> returnSalesService.finishReturnSales(200L), WM_RETURN_SALES_NO_LINE);
    }

    @Test
    public void testFinishReturnSales_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVING.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        returnSalesService.finishReturnSales(200L);
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.APPROVED.getStatus(), captor.getValue().getStatus());
    }

    // ========== stockReturnSales ==========

    @Test
    public void testStockReturnSales_notApproved() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> returnSalesService.stockReturnSales(200L), WM_RETURN_SALES_STATUS_NOT_APPROVED);
    }

    @Test
    public void testStockReturnSales_noLine() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVED.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.emptyList());
        when(returnSalesDetailService.getReturnSalesDetailListByReturnId(200L)).thenReturn(Collections.emptyList());
        returnSalesService.stockReturnSales(200L);
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.FINISHED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testStockReturnSales_quantityMatch() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVED.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(returnSalesDetailService.getReturnSalesDetailListByReturnId(200L)).thenReturn(Arrays.asList(
                buildDetail(11L, 1L, "6"), buildDetail(12L, 1L, "4")));
        returnSalesService.stockReturnSales(200L);
        verify(wmTransactionService).createTransactionList(anyList());
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.FINISHED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testStockReturnSales_transactionContent() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVED.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.emptyList());
        when(returnSalesDetailService.getReturnSalesDetailListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildDetail(11L, 1L, "8")));
        returnSalesService.stockReturnSales(200L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesWmTransactionSaveReqDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(wmTransactionService).createTransactionList(captor.capture());
        List<MesWmTransactionSaveReqDTO> dtoList = captor.getValue();
        assertEquals(1, dtoList.size());
        assertEquals(0, dtoList.get(0).getQuantity().compareTo(new BigDecimal("8")));
        assertEquals(200L, dtoList.get(0).getBizId());
    }

    @Test
    public void testStockReturnSales_quantityMismatch() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.APPROVED.getStatus()));
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(returnSalesDetailService.getReturnSalesDetailListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildDetail(11L, 1L, "3")));
        when(itemService.validateItemExistsAndEnable(10L))
                .thenReturn(new MesMdItemDO().setId(10L).setCode("I001").setName("物料A"));
        assertServiceException(() -> returnSalesService.stockReturnSales(200L), WM_RETURN_SALES_DETAIL_QUANTITY_MISMATCH);
        verify(returnSalesMapper, never()).updateById(any(MesWmReturnSalesDO.class));
    }

    // ========== cancelReturnSales ==========

    @Test
    public void testCancelReturnSales_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        returnSalesService.cancelReturnSales(200L);
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.CANCELED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testCancelReturnSales_finished() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> returnSalesService.cancelReturnSales(200L), WM_RETURN_SALES_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCancelReturnSales_canceled() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> returnSalesService.cancelReturnSales(200L), WM_RETURN_SALES_CANCEL_NOT_ALLOWED);
    }

    // ========== checkReturnSalesQuantity ==========

    @Test
    public void testCheckReturnSalesQuantity_true() {
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(returnSalesDetailService.getReturnSalesDetailListByLineId(1L)).thenReturn(Arrays.asList(
                buildDetail(11L, 1L, "6"), buildDetail(12L, 1L, "4")));
        assertTrue(returnSalesService.checkReturnSalesQuantity(200L));
    }

    @Test
    public void testCheckReturnSalesQuantity_false() {
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.singletonList(
                buildLine(1L, MesWmQualityStatusEnum.PASS.getStatus(), "10")));
        when(returnSalesDetailService.getReturnSalesDetailListByLineId(1L)).thenReturn(Collections.singletonList(
                buildDetail(11L, 1L, "3")));
        assertFalse(returnSalesService.checkReturnSalesQuantity(200L));
    }

    @Test
    public void testCheckReturnSalesQuantity_noLine() {
        when(returnSalesLineService.getReturnSalesLineListByReturnId(200L)).thenReturn(Collections.emptyList());
        assertTrue(returnSalesService.checkReturnSalesQuantity(200L));
    }

    // ========== validate / updateStatus ==========

    @Test
    public void testValidateReturnSalesExists_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnSalesService.validateReturnSalesExists(200L));
    }

    @Test
    public void testValidateReturnSalesExists_notExists() {
        when(returnSalesMapper.selectById(200L)).thenReturn(null);
        assertServiceException(() -> returnSalesService.validateReturnSalesExists(200L), WM_RETURN_SALES_NOT_EXISTS);
    }

    @Test
    public void testValidateReturnSalesExistsAndPrepare_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        assertNotNull(returnSalesService.validateReturnSalesExistsAndPrepare(200L));
    }

    @Test
    public void testValidateReturnSalesExistsAndPrepare_notPrepare() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.CONFIRMED.getStatus()));
        assertServiceException(() -> returnSalesService.validateReturnSalesExistsAndPrepare(200L),
                WM_RETURN_SALES_STATUS_NOT_PREPARE);
    }

    @Test
    public void testUpdateReturnSalesStatus_success() {
        when(returnSalesMapper.selectById(200L))
                .thenReturn(buildReturnSales(MesWmReturnSalesStatusEnum.PREPARE.getStatus()));
        returnSalesService.updateReturnSalesStatus(200L, MesWmReturnSalesStatusEnum.APPROVING.getStatus());
        ArgumentCaptor<MesWmReturnSalesDO> captor = ArgumentCaptor.forClass(MesWmReturnSalesDO.class);
        verify(returnSalesMapper).updateById(captor.capture());
        assertEquals(MesWmReturnSalesStatusEnum.APPROVING.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testUpdateReturnSalesStatus_notExists() {
        when(returnSalesMapper.selectById(200L)).thenReturn(null);
        assertServiceException(() -> returnSalesService.updateReturnSalesStatus(200L,
                MesWmReturnSalesStatusEnum.APPROVING.getStatus()), WM_RETURN_SALES_NOT_EXISTS);
    }

}
