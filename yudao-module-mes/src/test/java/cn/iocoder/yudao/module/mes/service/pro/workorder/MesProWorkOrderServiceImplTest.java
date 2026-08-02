package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemBatchConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemBatchConfigService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MesProWorkOrderServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesProWorkOrderServiceImpl.class)
public class MesProWorkOrderServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesProWorkOrderMapper workOrderMapper;
    @MockitoBean
    private MesProWorkOrderBomService workOrderBomService;
    @MockitoBean
    private MesMdItemService itemService;
    @MockitoBean
    private MesMdItemBatchConfigService itemBatchConfigService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesProTaskService taskService;

    @Resource
    private MesProWorkOrderServiceImpl workOrderService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesProWorkOrderDO workOrder = inv.getArgument(0);
            workOrder.setId(workOrder.getId() == null ? 100L : workOrder.getId());
            return 1;
        }).when(workOrderMapper).insert(any(MesProWorkOrderDO.class));
        when(workOrderMapper.updateById(any(MesProWorkOrderDO.class))).thenReturn(1);
        when(workOrderMapper.deleteById(anyLong())).thenReturn(1);
        // 默认无子工单
        when(workOrderMapper.selectCount(ArgumentMatchers.<SFunction<MesProWorkOrderDO, ?>>any(), any()))
                .thenReturn(0L);
    }

    private MesProWorkOrderDO buildWorkOrder() {
        return new MesProWorkOrderDO().setId(100L).setCode("WO-001").setName("工单A")
                .setProductId(200L).setQuantity(new BigDecimal("10"))
                .setParentId(MesProWorkOrderDO.PARENT_ID_NULL)
                .setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus());
    }

    private MesProWorkOrderSaveReqVO buildSaveReq() {
        return new MesProWorkOrderSaveReqVO().setCode("WO-001").setName("工单A")
                .setType(1).setOrderSourceType(1).setProductId(200L)
                .setQuantity(new BigDecimal("10")).setRequestDate(LocalDateTime.now());
    }

    // ========== createWorkOrder ==========

    @Test
    public void testCreateWorkOrder_success() {
        MesProWorkOrderSaveReqVO reqVO = buildSaveReq();
        Long id = workOrderService.createWorkOrder(reqVO);

        assertEquals(100L, id);
        // 默认父工单编号
        assertEquals(MesProWorkOrderDO.PARENT_ID_NULL, reqVO.getParentId());
        verify(workOrderMapper).insert(any(MesProWorkOrderDO.class));
        verify(workOrderBomService).generateWorkOrderBom(eq(100L), any(MesProWorkOrderSaveReqVO.class), eq(false));
        verify(barcodeService).autoGenerateBarcode(eq(BarcodeBizTypeEnum.WORKORDER.getValue()),
                eq(100L), eq("WO-001"), eq("工单A"));
        verify(itemService).validateItemExists(200L);
    }

    @Test
    public void testCreateWorkOrder_withParentId() {
        MesProWorkOrderSaveReqVO reqVO = buildSaveReq().setParentId(66L);
        Long id = workOrderService.createWorkOrder(reqVO);

        assertEquals(100L, id);
        assertEquals(66L, reqVO.getParentId());
    }

    @Test
    public void testCreateWorkOrder_codeDuplicate() {
        when(workOrderMapper.selectByCode("WO-001")).thenReturn(buildWorkOrder());
        assertServiceException(() -> workOrderService.createWorkOrder(buildSaveReq()), PRO_WORK_ORDER_CODE_DUPLICATE);
    }

    @Test
    public void testCreateWorkOrder_codeNull() {
        // 编码为空时跳过唯一校验
        MesProWorkOrderSaveReqVO reqVO = buildSaveReq().setCode(null);
        assertEquals(100L, workOrderService.createWorkOrder(reqVO));
        verify(workOrderMapper, never()).selectByCode(any());
    }

    @Test
    public void testCreateWorkOrder_clientRequired() {
        // 产品配置了客户批次属性，但未传客户
        when(itemBatchConfigService.getItemBatchConfigByItemId(200L))
                .thenReturn(new MesMdItemBatchConfigDO().setItemId(200L).setClientFlag(true));
        assertServiceException(() -> workOrderService.createWorkOrder(buildSaveReq()), MD_CLIENT_NOT_EXISTS);
    }

    @Test
    public void testCreateWorkOrder_clientFlagWithClientId() {
        when(itemBatchConfigService.getItemBatchConfigByItemId(200L))
                .thenReturn(new MesMdItemBatchConfigDO().setItemId(200L).setClientFlag(true));
        assertEquals(100L, workOrderService.createWorkOrder(buildSaveReq().setClientId(300L)));
    }

    @Test
    public void testCreateWorkOrder_clientFlagFalse() {
        when(itemBatchConfigService.getItemBatchConfigByItemId(200L))
                .thenReturn(new MesMdItemBatchConfigDO().setItemId(200L).setClientFlag(false));
        assertEquals(100L, workOrderService.createWorkOrder(buildSaveReq()));
    }

    // ========== updateWorkOrder ==========

    @Test
    public void testUpdateWorkOrder_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.updateWorkOrder(buildSaveReq().setId(100L).setName("工单B"));

        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
        // 产品与数量都未变更，不重新生成 BOM
        verify(workOrderBomService, never()).generateWorkOrderBom(anyLong(), any(MesProWorkOrderSaveReqVO.class), anyBoolean());
    }

    @Test
    public void testUpdateWorkOrder_productChanged() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.updateWorkOrder(buildSaveReq().setId(100L).setProductId(999L));

        verify(workOrderBomService).generateWorkOrderBom(eq(100L), any(MesProWorkOrderSaveReqVO.class), eq(true));
        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
    }

    @Test
    public void testUpdateWorkOrder_quantityChanged() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.updateWorkOrder(buildSaveReq().setId(100L).setQuantity(new BigDecimal("20")));

        verify(workOrderBomService).generateWorkOrderBom(eq(100L), any(MesProWorkOrderSaveReqVO.class), eq(true));
    }

    @Test
    public void testUpdateWorkOrder_notExists() {
        when(workOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> workOrderService.updateWorkOrder(buildSaveReq().setId(100L)),
                PRO_WORK_ORDER_NOT_EXISTS);
    }

    @Test
    public void testUpdateWorkOrder_notPrepare() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));
        assertServiceException(() -> workOrderService.updateWorkOrder(buildSaveReq().setId(100L)),
                PRO_WORK_ORDER_NOT_PREPARE);
    }

    @Test
    public void testUpdateWorkOrder_codeDuplicate() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        // 编码被另一个工单占用
        when(workOrderMapper.selectByCode("WO-001")).thenReturn(buildWorkOrder().setId(101L));
        assertServiceException(() -> workOrderService.updateWorkOrder(buildSaveReq().setId(100L)),
                PRO_WORK_ORDER_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateWorkOrder_codeBelongToSelf() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        // 编码属于自己，允许更新
        when(workOrderMapper.selectByCode("WO-001")).thenReturn(buildWorkOrder());

        workOrderService.updateWorkOrder(buildSaveReq().setId(100L));

        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
    }

    // ========== deleteWorkOrder ==========

    @Test
    public void testDeleteWorkOrder_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.deleteWorkOrder(100L);

        verify(workOrderMapper).deleteById(100L);
        verify(workOrderBomService).deleteWorkOrderBomByWorkOrderId(100L);
    }

    @Test
    public void testDeleteWorkOrder_notExists() {
        when(workOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> workOrderService.deleteWorkOrder(100L), PRO_WORK_ORDER_NOT_EXISTS);
    }

    @Test
    public void testDeleteWorkOrder_notPrepare() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus()));
        assertServiceException(() -> workOrderService.deleteWorkOrder(100L), PRO_WORK_ORDER_NOT_PREPARE);
    }

    @Test
    public void testDeleteWorkOrder_hasChildren() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        when(workOrderMapper.selectCount(ArgumentMatchers.<SFunction<MesProWorkOrderDO, ?>>any(), any()))
                .thenReturn(1L);
        assertServiceException(() -> workOrderService.deleteWorkOrder(100L), PRO_WORK_ORDER_HAS_CHILDREN);
    }

    // ========== validate ==========

    @Test
    public void testValidateWorkOrderExists_exists() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        assertNotNull(workOrderService.validateWorkOrderExists(100L));
    }

    @Test
    public void testValidateWorkOrderExists_notExists() {
        when(workOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> workOrderService.validateWorkOrderExists(100L), PRO_WORK_ORDER_NOT_EXISTS);
    }

    @Test
    public void testValidateWorkOrderConfirmed_confirmed() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));
        assertNotNull(workOrderService.validateWorkOrderConfirmed(100L));
    }

    @Test
    public void testValidateWorkOrderConfirmed_notConfirmed() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        assertServiceException(() -> workOrderService.validateWorkOrderConfirmed(100L), PRO_WORK_ORDER_NOT_CONFIRMED);
    }

    // ========== get / list / page ==========

    @Test
    public void testGetWorkOrderById() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        assertNotNull(workOrderService.getWorkOrder(100L));
    }

    @Test
    public void testGetWorkOrderById_notExists() {
        when(workOrderMapper.selectById(100L)).thenReturn(null);
        assertNull(workOrderService.getWorkOrder(100L));
    }

    @Test
    public void testGetWorkOrderByCode() {
        when(workOrderMapper.selectByCode("WO-001")).thenReturn(buildWorkOrder());
        assertEquals("WO-001", workOrderService.getWorkOrder("WO-001").getCode());
    }

    @Test
    public void testGetWorkOrderPage() {
        PageResult<MesProWorkOrderDO> page = new PageResult<>(Collections.singletonList(buildWorkOrder()), 1L);
        when(workOrderMapper.selectPage(any(MesProWorkOrderPageReqVO.class))).thenReturn(page);
        assertEquals(1L, workOrderService.getWorkOrderPage(new MesProWorkOrderPageReqVO()).getTotal());
    }

    @Test
    public void testGetWorkOrderList_empty() {
        assertTrue(workOrderService.getWorkOrderList(Collections.emptyList()).isEmpty());
        assertTrue(workOrderService.getWorkOrderList(null).isEmpty());
    }

    @Test
    public void testGetWorkOrderList_nonEmpty() {
        when(workOrderMapper.selectByIds(any())).thenReturn(Arrays.asList(buildWorkOrder()));
        assertEquals(1, workOrderService.getWorkOrderList(Arrays.asList(100L)).size());
    }

    // ========== confirm / finish / cancel ==========

    @Test
    public void testConfirmWorkOrder_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.confirmWorkOrder(100L);

        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
    }

    @Test
    public void testConfirmWorkOrder_notPrepare() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));
        assertServiceException(() -> workOrderService.confirmWorkOrder(100L), PRO_WORK_ORDER_NOT_PREPARE);
    }

    @Test
    public void testFinishWorkOrder_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));

        workOrderService.finishWorkOrder(100L);

        verify(taskService).finishTaskByOrderId(100L);
        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
    }

    @Test
    public void testFinishWorkOrder_notConfirmed() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        assertServiceException(() -> workOrderService.finishWorkOrder(100L), PRO_WORK_ORDER_NOT_CONFIRMED);
        verify(taskService, never()).finishTaskByOrderId(anyLong());
    }

    @Test
    public void testCancelWorkOrder_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(
                buildWorkOrder().setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));

        workOrderService.cancelWorkOrder(100L);

        verify(taskService).cancelTaskByOrderId(100L);
        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
    }

    @Test
    public void testCancelWorkOrder_notConfirmed() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());
        assertServiceException(() -> workOrderService.cancelWorkOrder(100L), PRO_WORK_ORDER_NOT_CONFIRMED);
        verify(taskService, never()).cancelTaskByOrderId(anyLong());
    }

    // ========== 其它 ==========

    @Test
    public void testUpdateProducedQuantity_success() {
        when(workOrderMapper.selectById(100L)).thenReturn(buildWorkOrder());

        workOrderService.updateProducedQuantity(100L, new BigDecimal("5"));

        verify(workOrderMapper).updateProducedQuantity(eq(100L), any(BigDecimal.class));
    }

    @Test
    public void testUpdateProducedQuantity_notExists() {
        when(workOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> workOrderService.updateProducedQuantity(100L, new BigDecimal("5")),
                PRO_WORK_ORDER_NOT_EXISTS);
    }

    @Test
    public void testGetWorkOrderCountByVendorId() {
        when(workOrderMapper.selectCountByVendorId(400L)).thenReturn(2L);
        assertEquals(2L, workOrderService.getWorkOrderCountByVendorId(400L));
    }

}
