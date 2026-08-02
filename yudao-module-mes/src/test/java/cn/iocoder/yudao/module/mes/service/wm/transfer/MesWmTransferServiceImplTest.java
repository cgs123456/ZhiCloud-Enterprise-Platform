package cn.iocoder.yudao.module.mes.service.wm.transfer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.wm.transfer.vo.MesWmTransferPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.transfer.vo.MesWmTransferSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmTransferStatusEnum;
import cn.iocoder.yudao.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.iocoder.yudao.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmTransferServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmTransferServiceImpl.class)
public class MesWmTransferServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmTransferMapper transferMapper;
    @MockitoBean
    private MesWmTransferLineService transferLineService;
    @MockitoBean
    private MesWmTransferDetailService transferDetailService;
    @MockitoBean
    private MesWmMaterialStockService materialStockService;
    @MockitoBean
    private MesWmTransactionService wmTransactionService;

    @Resource
    private MesWmTransferServiceImpl transferService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmTransferDO transfer = inv.getArgument(0);
            transfer.setId(transfer.getId() == null ? 100L : transfer.getId());
            return 1;
        }).when(transferMapper).insert(any(MesWmTransferDO.class));
        when(transferMapper.updateById(any(MesWmTransferDO.class))).thenReturn(1);
        when(transferMapper.deleteById(anyLong())).thenReturn(1);
        // 库存事务默认返回一个 ID
        when(wmTransactionService.createTransaction(any(MesWmTransactionSaveReqDTO.class))).thenReturn(9527L);
    }

    // ==================== 构造数据 ====================

    private MesWmTransferDO buildTransfer(Integer status) {
        return new MesWmTransferDO().setId(100L).setCode("TR001").setName("转移单A")
                .setType(1).setDeliveryFlag(false).setConfirmFlag(false)
                .setTransferDate(LocalDateTime.now()).setStatus(status);
    }

    private MesWmTransferSaveReqVO buildSaveReq() {
        return new MesWmTransferSaveReqVO().setCode("TR001").setName("转移单A")
                .setType(1).setDeliveryFlag(false).setTransferDate(LocalDateTime.now());
    }

    private MesWmTransferLineDO buildLine() {
        return new MesWmTransferLineDO().setId(200L).setTransferId(100L).setMaterialStockId(300L)
                .setItemId(400L).setQuantity(new BigDecimal("10")).setBatchId(500L)
                .setFromWarehouseId(1L).setFromLocationId(2L).setFromAreaId(3L);
    }

    private MesWmTransferDetailDO buildDetail(BigDecimal quantity) {
        return new MesWmTransferDetailDO().setId(600L).setLineId(200L).setTransferId(100L)
                .setItemId(400L).setQuantity(quantity).setBatchId(500L)
                .setToWarehouseId(11L).setToLocationId(12L).setToAreaId(13L);
    }

    // ==================== createTransfer ====================

    @Test
    public void testCreateTransfer_success() {
        Long id = transferService.createTransfer(buildSaveReq());
        assertEquals(100L, id);
        verify(transferMapper).insert(any(MesWmTransferDO.class));
    }

    @Test
    public void testCreateTransfer_codeDuplicate() {
        when(transferMapper.selectByCode("TR001")).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> transferService.createTransfer(buildSaveReq()), WM_TRANSFER_CODE_DUPLICATE);
    }

    // ==================== updateTransfer ====================

    @Test
    public void testUpdateTransfer_success() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        transferService.updateTransfer(buildSaveReq().setId(100L).setName("转移单B"));
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
    }

    @Test
    public void testUpdateTransfer_codeSelf() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        when(transferMapper.selectByCode("TR001")).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        transferService.updateTransfer(buildSaveReq().setId(100L));
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
    }

    @Test
    public void testUpdateTransfer_codeDuplicate() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        when(transferMapper.selectByCode("TR001")).thenReturn(
                buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()).setId(101L));
        assertServiceException(() -> transferService.updateTransfer(buildSaveReq().setId(100L)),
                WM_TRANSFER_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.updateTransfer(buildSaveReq().setId(100L)), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testUpdateTransfer_notDraft() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> transferService.updateTransfer(buildSaveReq().setId(100L)), WM_TRANSFER_NOT_DRAFT);
    }

    // ==================== deleteTransfer ====================

    @Test
    public void testDeleteTransfer_success() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        transferService.deleteTransfer(100L);
        verify(transferDetailService).deleteTransferDetailByTransferId(100L);
        verify(transferLineService).deleteTransferLineByTransferId(100L);
        verify(transferMapper).deleteById(100L);
    }

    @Test
    public void testDeleteTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.deleteTransfer(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testDeleteTransfer_notDraft() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> transferService.deleteTransfer(100L), WM_TRANSFER_NOT_DRAFT);
    }

    // ==================== get / page ====================

    @Test
    public void testGetTransfer() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        MesWmTransferDO transfer = transferService.getTransfer(100L);
        assertNotNull(transfer);
        assertEquals("TR001", transfer.getCode());
    }

    @Test
    public void testGetTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertNull(transferService.getTransfer(100L));
    }

    @Test
    public void testGetTransferPage() {
        PageResult<MesWmTransferDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(transferMapper.selectPage(any(MesWmTransferPageReqVO.class))).thenReturn(page);
        assertEquals(0, transferService.getTransferPage(new MesWmTransferPageReqVO()).getTotal());
    }

    // ==================== submitTransfer ====================

    @Test
    public void testSubmitTransfer_noLine() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> transferService.submitTransfer(100L), WM_TRANSFER_NO_LINE);
    }

    @Test
    public void testSubmitTransfer_notDraft() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> transferService.submitTransfer(100L), WM_TRANSFER_NOT_DRAFT);
    }

    @Test
    public void testSubmitTransfer_notDelivery() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus());
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        transferService.submitTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.APPROVING.getStatus(), transfer.getStatus());
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
        verify(materialStockService, never()).updateMaterialStockFrozen(any(), anyBoolean());
    }

    @Test
    public void testSubmitTransfer_deliveryUnconfirmed() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()).setDeliveryFlag(true);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        transferService.submitTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.UNCONFIRMED.getStatus(), transfer.getStatus());
        assertFalse(transfer.getConfirmFlag());
        verify(materialStockService).updateMaterialStockFrozen(any(), eq(true));
    }

    @Test
    public void testSubmitTransfer_deliveryConfirmed() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus())
                .setDeliveryFlag(true).setConfirmFlag(true);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        transferService.submitTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.APPROVING.getStatus(), transfer.getStatus());
        verify(materialStockService, never()).updateMaterialStockFrozen(any(), anyBoolean());
    }

    // ==================== confirmTransfer ====================

    @Test
    public void testConfirmTransfer_success() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.UNCONFIRMED.getStatus());
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        transferService.confirmTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.APPROVING.getStatus(), transfer.getStatus());
        assertTrue(transfer.getConfirmFlag());
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
    }

    @Test
    public void testConfirmTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.confirmTransfer(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testConfirmTransfer_notUnconfirmed() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> transferService.confirmTransfer(100L), WM_TRANSFER_NOT_CONFIRMED);
    }

    // ==================== stockTransfer ====================

    @Test
    public void testStockTransfer_success() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus());
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(transferDetailService.getTransferDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("4")), buildDetail(new BigDecimal("6"))));
        transferService.stockTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.APPROVED.getStatus(), transfer.getStatus());
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
    }

    @Test
    public void testStockTransfer_notApproving() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        assertServiceException(() -> transferService.stockTransfer(100L), WM_TRANSFER_NOT_APPROVING);
    }

    @Test
    public void testStockTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.stockTransfer(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testStockTransfer_noLine() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()));
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> transferService.stockTransfer(100L), WM_TRANSFER_NO_LINE);
    }

    @Test
    public void testStockTransfer_quantityMismatch() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()));
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(transferDetailService.getTransferDetailListByLineId(200L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("3"))));
        assertServiceException(() -> transferService.stockTransfer(100L), WM_TRANSFER_DETAIL_QUANTITY_MISMATCH);
    }

    // ==================== finishTransfer ====================

    @Test
    public void testFinishTransfer_success() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus());
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(transferDetailService.getTransferDetailListByTransferId(100L))
                .thenReturn(Arrays.asList(buildDetail(new BigDecimal("10"))));
        transferService.finishTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.FINISHED.getStatus(), transfer.getStatus());
        // 一条明细 -> 一笔出库 + 一笔入库
        verify(wmTransactionService, times(2)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(materialStockService, never()).updateMaterialStockFrozen(any(), anyBoolean());
    }

    @Test
    public void testFinishTransfer_deliveryUnfreeze() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus()).setDeliveryFlag(true);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        when(transferDetailService.getTransferDetailListByTransferId(100L)).thenReturn(Collections.emptyList());
        transferService.finishTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.FINISHED.getStatus(), transfer.getStatus());
        // 没有明细，不产生事务
        verify(wmTransactionService, never()).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        verify(materialStockService).updateMaterialStockFrozen(any(), eq(false));
    }

    @Test
    public void testFinishTransfer_notApproved() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()));
        assertServiceException(() -> transferService.finishTransfer(100L), WM_TRANSFER_NOT_APPROVED);
    }

    @Test
    public void testFinishTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.finishTransfer(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testFinishTransfer_noLine() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus()));
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> transferService.finishTransfer(100L), WM_TRANSFER_NO_LINE);
    }

    // ==================== cancelTransfer ====================

    @Test
    public void testCancelTransfer_success() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus());
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        transferService.cancelTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.CANCELED.getStatus(), transfer.getStatus());
        verify(transferMapper).updateById(any(MesWmTransferDO.class));
    }

    @Test
    public void testCancelTransfer_deliveryUnfreeze() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()).setDeliveryFlag(true);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Arrays.asList(buildLine()));
        transferService.cancelTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.CANCELED.getStatus(), transfer.getStatus());
        verify(materialStockService).updateMaterialStockFrozen(any(), eq(false));
    }

    @Test
    public void testCancelTransfer_deliveryNoLine() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVING.getStatus()).setDeliveryFlag(true);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(Collections.emptyList());
        transferService.cancelTransfer(100L);
        assertEquals(MesWmTransferStatusEnum.CANCELED.getStatus(), transfer.getStatus());
        verify(materialStockService, never()).updateMaterialStockFrozen(any(), anyBoolean());
    }

    @Test
    public void testCancelTransfer_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.cancelTransfer(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testCancelTransfer_alreadyFinished() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> transferService.cancelTransfer(100L), WM_TRANSFER_ALREADY_FINISHED);
    }

    @Test
    public void testCancelTransfer_alreadyCanceled() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.CANCELED.getStatus()));
        assertServiceException(() -> transferService.cancelTransfer(100L), WM_TRANSFER_ALREADY_FINISHED);
    }

    // ==================== validateTransferEditable ====================

    @Test
    public void testValidateTransferEditable_success() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.PREPARE.getStatus()));
        MesWmTransferDO transfer = transferService.validateTransferEditable(100L);
        assertNotNull(transfer);
        assertEquals(100L, transfer.getId());
    }

    @Test
    public void testValidateTransferEditable_notExists() {
        when(transferMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> transferService.validateTransferEditable(100L), WM_TRANSFER_NOT_EXISTS);
    }

    @Test
    public void testValidateTransferEditable_notEditable() {
        when(transferMapper.selectById(100L)).thenReturn(buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus()));
        assertServiceException(() -> transferService.validateTransferEditable(100L), WM_TRANSFER_NOT_EDITABLE);
    }

    // ==================== 多行多明细组合场景 ====================

    @Test
    public void testFinishTransfer_multiLineMultiDetail() {
        MesWmTransferDO transfer = buildTransfer(MesWmTransferStatusEnum.APPROVED.getStatus());
        MesWmTransferLineDO line2 = buildLine().setId(201L).setQuantity(new BigDecimal("5"));
        List<MesWmTransferLineDO> lines = Arrays.asList(buildLine(), line2);
        when(transferMapper.selectById(100L)).thenReturn(transfer);
        when(transferLineService.getTransferLineListByTransferId(100L)).thenReturn(lines);
        when(transferDetailService.getTransferDetailListByTransferId(100L)).thenReturn(Arrays.asList(
                buildDetail(new BigDecimal("10")),
                buildDetail(new BigDecimal("5")).setId(601L).setLineId(201L)));
        transferService.finishTransfer(100L);
        // 两条明细 -> 4 笔事务
        verify(wmTransactionService, times(4)).createTransaction(any(MesWmTransactionSaveReqDTO.class));
        assertEquals(0, new BigDecimal("5").compareTo(line2.getQuantity()));
    }

}
