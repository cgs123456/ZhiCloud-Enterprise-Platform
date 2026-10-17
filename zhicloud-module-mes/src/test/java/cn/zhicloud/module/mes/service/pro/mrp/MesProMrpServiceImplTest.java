package cn.zhicloud.module.mes.service.pro.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpConvertRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.materialstock.vo.MesWmMaterialStockListReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.dal.mysql.pro.mrp.MesProMrpPlanMapper;
import cn.zhicloud.module.mes.dal.mysql.pro.mrp.MesProMrpResultMapper;
import cn.zhicloud.module.mes.enums.md.MesMrpLotSizeRuleEnum;
import cn.zhicloud.module.mes.enums.pro.MesProMrpPlanStatusEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import cn.zhicloud.module.mes.service.md.item.MesMdProductBomService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.zhicloud.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_CALCULATED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_DRAFT;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_CONFIRMED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NO_SUGGESTION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesProMrpServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesProMrpServiceImpl.class)
public class MesProMrpServiceImplTest extends BaseDbUnitTest {

    private static final Long PLAN_ID = 100L;
    private static final LocalDateTime PLAN_DATE = LocalDateTime.of(2026, 1, 15, 10, 0, 0);

    @MockitoBean
    private MesProMrpPlanMapper mrpPlanMapper;
    @MockitoBean
    private MesProMrpResultMapper mrpResultMapper;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesMdProductBomService productBomService;
    @MockitoBean
    private MesWmMaterialStockService materialStockService;
    @MockitoBean
    private MesMdItemService itemService;

    @Resource
    private MesProMrpServiceImpl mrpService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesProMrpPlanDO plan = inv.getArgument(0);
            plan.setId(plan.getId() == null ? PLAN_ID : plan.getId());
            return 1;
        }).when(mrpPlanMapper).insert(any(MesProMrpPlanDO.class));
        when(mrpPlanMapper.updateById(any(MesProMrpPlanDO.class))).thenReturn(1);
        when(mrpResultMapper.deleteByPlanId(anyLong())).thenReturn(0);
        // 默认：无已确认工单
        when(workOrderService.getWorkOrderPage(any(MesProWorkOrderPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        // 默认：无 BOM 子件
        when(productBomService.getProductBomListByItemId(anyLong())).thenReturn(Collections.emptyList());
        // 默认：无库存
        when(materialStockService.getMaterialStockList(any(MesWmMaterialStockListReqVO.class)))
                .thenReturn(Collections.emptyList());
        // 默认：无物料主数据
        when(itemService.getItemMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    // ========== 构造方法 ==========

    private MesProMrpPlanDO buildPlan(Integer status) {
        return MesProMrpPlanDO.builder().id(PLAN_ID).planNo("MRP-001")
                .planDate(PLAN_DATE).status(status).build();
    }

    private MesProMrpPlanCreateReqVO buildCreateReq() {
        return new MesProMrpPlanCreateReqVO().setPlanNo("MRP-001").setRemark("备注");
    }

    private MesProWorkOrderDO buildWorkOrder(Long productId, String quantity) {
        return MesProWorkOrderDO.builder().id(1L).code("WO-001").productId(productId)
                .quantity(quantity == null ? null : new BigDecimal(quantity)).build();
    }

    private void mockWorkOrders(MesProWorkOrderDO... workOrders) {
        List<MesProWorkOrderDO> list = Arrays.asList(workOrders);
        when(workOrderService.getWorkOrderPage(any(MesProWorkOrderPageReqVO.class)))
                .thenReturn(new PageResult<>(list, (long) list.size()));
    }

    private void mockItemMap(MesMdItemDO... items) {
        Map<Long, MesMdItemDO> map = new HashMap<>();
        for (MesMdItemDO item : items) {
            map.put(item.getId(), item);
        }
        when(itemService.getItemMap(anyCollection())).thenReturn(map);
    }

    // ========== createMrpPlan ==========

    @Test
    public void testCreateMrpPlan_success() {
        when(mrpPlanMapper.selectByPlanNo("MRP-001")).thenReturn(null);
        Long id = mrpService.createMrpPlan(buildCreateReq());
        assertEquals(PLAN_ID, id);
        verify(mrpPlanMapper).insert(any(MesProMrpPlanDO.class));
    }

    @Test
    public void testCreateMrpPlan_planNoNull() {
        // planNo 为空时跳过唯一性校验
        Long id = mrpService.createMrpPlan(new MesProMrpPlanCreateReqVO());
        assertEquals(PLAN_ID, id);
        verify(mrpPlanMapper, never()).selectByPlanNo(any());
    }

    @Test
    public void testCreateMrpPlan_planNoDuplicate() {
        when(mrpPlanMapper.selectByPlanNo("MRP-001")).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        assertServiceException(() -> mrpService.createMrpPlan(buildCreateReq()), PRO_MRP_PLAN_NOT_EXISTS);
    }

    // ========== calculateMrp：异常分支 ==========

    @Test
    public void testCalculateMrp_planNotExists() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(null);
        assertServiceException(() -> mrpService.calculateMrp(PLAN_ID), PRO_MRP_PLAN_NOT_EXISTS);
    }

    @Test
    public void testCalculateMrp_notDraft() {
        when(mrpPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProMrpPlanStatusEnum.CALCULATED.getStatus()));
        assertServiceException(() -> mrpService.calculateMrp(PLAN_ID), PRO_MRP_PLAN_NOT_DRAFT);
    }

    // ========== calculateMrp：正常分支 ==========

    @Test
    public void testCalculateMrp_noWorkOrders() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertTrue(results.isEmpty());
        verify(mrpPlanMapper).updateById(any(MesProMrpPlanDO.class));
        verify(mrpResultMapper, never()).deleteByPlanId(anyLong());
    }

    @Test
    public void testCalculateMrp_workOrderWithNullProductId() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(null, "10"));
        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertTrue(results.isEmpty());
        verify(mrpResultMapper).deleteByPlanId(PLAN_ID);
        verify(mrpResultMapper, never()).insertBatch(anyCollection());
    }

    @Test
    public void testCalculateMrp_workOrderWithNullQuantity() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, null));
        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testCalculateMrp_singleItemNoBom() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        MesProMrpResultDO result = results.get(0);
        assertEquals(PLAN_ID, result.getPlanId());
        assertEquals(1L, result.getProductId());
        assertEquals(0, result.getRequirementQty().compareTo(new BigDecimal("10")));
        assertEquals(0, result.getStockQty().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getNetRequirement().compareTo(new BigDecimal("10")));
        assertEquals(0, result.getSafetyStock().compareTo(BigDecimal.ZERO));
        assertEquals(MesMrpLotSizeRuleEnum.LFL.getCode(), result.getLotSizeRule());
        assertEquals(0, result.getPlannedOrderQty().compareTo(new BigDecimal("10")));
        // 默认提前期 7 天
        assertEquals(PLAN_DATE.minusDays(7), result.getPlannedOrderDate());
        verify(mrpResultMapper).deleteByPlanId(PLAN_ID);
        verify(mrpResultMapper).insertBatch(anyCollection());
        verify(mrpPlanMapper).updateById(any(MesProMrpPlanDO.class));
    }

    @Test
    public void testCalculateMrp_bomExpansion() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        // 物料 1 -> 子件 2，用量 3
        when(productBomService.getProductBomListByItemId(1L)).thenReturn(Collections.singletonList(
                MesMdProductBomDO.builder().id(1L).itemId(1L).bomItemId(2L)
                        .quantity(new BigDecimal("3")).build()));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(2, results.size());
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        results.forEach(r -> qtyMap.put(r.getProductId(), r.getRequirementQty()));
        assertEquals(0, qtyMap.get(1L).compareTo(new BigDecimal("10")));
        assertEquals(0, qtyMap.get(2L).compareTo(new BigDecimal("30")));
    }

    @Test
    public void testCalculateMrp_bomDetailWithNullFields() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        // 子件编号为空 / 用量为空，均跳过
        when(productBomService.getProductBomListByItemId(1L)).thenReturn(Arrays.asList(
                MesMdProductBomDO.builder().id(1L).itemId(1L).bomItemId(null)
                        .quantity(new BigDecimal("3")).build(),
                MesMdProductBomDO.builder().id(2L).itemId(1L).bomItemId(2L)
                        .quantity(null).build()));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getProductId());
    }

    @Test
    public void testCalculateMrp_bomCircular() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        // 1 -> 2 -> 1 形成闭环，visited 保证不会无限递归
        when(productBomService.getProductBomListByItemId(1L)).thenReturn(Collections.singletonList(
                MesMdProductBomDO.builder().id(1L).itemId(1L).bomItemId(2L)
                        .quantity(new BigDecimal("2")).build()));
        when(productBomService.getProductBomListByItemId(2L)).thenReturn(Collections.singletonList(
                MesMdProductBomDO.builder().id(2L).itemId(2L).bomItemId(1L)
                        .quantity(BigDecimal.ONE).build()));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(2, results.size());
    }

    @Test
    public void testCalculateMrp_stockCoversRequirement() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        when(materialStockService.batchGetStockQuantity(any())).thenReturn(Map.of(1L, new BigDecimal("50")));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        MesProMrpResultDO result = results.get(0);
        assertEquals(0, result.getStockQty().compareTo(new BigDecimal("50")));
        assertEquals(0, result.getNetRequirement().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getPlannedOrderQty().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCalculateMrp_stockWithNullQuantity() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "100"));
        when(materialStockService.batchGetStockQuantity(any())).thenReturn(Map.of(1L, new BigDecimal("30")));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getStockQty().compareTo(new BigDecimal("30")));
        assertEquals(0, results.get(0).getNetRequirement().compareTo(new BigDecimal("70")));
    }

    @Test
    public void testCalculateMrp_safetyStockAndFoq() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        mockItemMap(MesMdItemDO.builder().id(1L).code("I001").name("物料1").lowLevelCode(0)
                .safetyStock(new BigDecimal("5"))
                .lotSizeRule(MesMrpLotSizeRuleEnum.FOQ.getCode())
                .fixedLotSize(new BigDecimal("25")).build());

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        MesProMrpResultDO result = results.get(0);
        assertEquals(0, result.getSafetyStock().compareTo(new BigDecimal("5")));
        // 净需求 = 10 - 0 + 5 = 15，FOQ 固定批量 25 -> 向上取整 1 倍 = 25
        assertEquals(0, result.getNetRequirement().compareTo(new BigDecimal("15")));
        assertEquals(MesMrpLotSizeRuleEnum.FOQ.getCode(), result.getLotSizeRule());
        assertEquals(0, result.getPlannedOrderQty().compareTo(new BigDecimal("25")));
    }

    @Test
    public void testCalculateMrp_multiplesRule() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        mockItemMap(MesMdItemDO.builder().id(1L).code("I001").name("物料1").lowLevelCode(0)
                .lotSizeRule(MesMrpLotSizeRuleEnum.MULTIPLES.getCode())
                .lotSizeMultiple(new BigDecimal("4")).build());

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        // 净需求 10，按 4 的倍数向上取整 -> 12
        assertEquals(0, results.get(0).getPlannedOrderQty().compareTo(new BigDecimal("12")));
    }

    @Test
    public void testCalculateMrp_scrapRate() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        mockItemMap(MesMdItemDO.builder().id(1L).code("I001").name("物料1").lowLevelCode(0)
                .scrapRate(new BigDecimal("10")).build());

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        // 10 * (1 + 10/100) = 11
        assertEquals(0, results.get(0).getPlannedOrderQty().compareTo(new BigDecimal("11")));
    }

    @Test
    public void testCalculateMrp_leadTimeDays() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        mockItemMap(MesMdItemDO.builder().id(1L).code("I001").name("物料1").lowLevelCode(0)
                .leadTimeDays(3).build());

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(PLAN_DATE.minusDays(3), results.get(0).getPlannedOrderDate());
    }

    @Test
    public void testCalculateMrp_sortByLowLevelCode() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        mockWorkOrders(buildWorkOrder(1L, "10"));
        when(productBomService.getProductBomListByItemId(1L)).thenReturn(Collections.singletonList(
                MesMdProductBomDO.builder().id(1L).itemId(1L).bomItemId(2L)
                        .quantity(new BigDecimal("2")).build()));
        // 物料 1 无主数据（lowLevelCode 视为最大），物料 2 低层码 1 -> 物料 2 先算
        mockItemMap(MesMdItemDO.builder().id(2L).code("I002").name("物料2").lowLevelCode(1).build());

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(2, results.size());
        assertEquals(2L, results.get(0).getProductId());
        assertEquals(1L, results.get(1).getProductId());
    }

    @Test
    public void testCalculateMrp_planDateNull() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(MesProMrpPlanDO.builder().id(PLAN_ID)
                .planNo("MRP-001").planDate(null)
                .status(MesProMrpPlanStatusEnum.DRAFT.getStatus()).build());
        mockWorkOrders(buildWorkOrder(1L, "10"));

        List<MesProMrpResultDO> results = mrpService.calculateMrp(PLAN_ID);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getPlannedOrderDate());
    }

    // ========== get / page ==========

    @Test
    public void testGetMrpResult() {
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Collections.singletonList(
                MesProMrpResultDO.builder().id(1L).planId(PLAN_ID).productId(1L).build()));
        List<MesProMrpResultDO> results = mrpService.getMrpResult(PLAN_ID);
        assertEquals(1, results.size());
        assertEquals(PLAN_ID, results.get(0).getPlanId());
    }

    @Test
    public void testGetMrpPlan() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));
        assertNotNull(mrpService.getMrpPlan(PLAN_ID));
    }

    @Test
    public void testGetMrpPlan_notExists() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(null);
        assertNull(mrpService.getMrpPlan(PLAN_ID));
    }

    @Test
    public void testGetMrpPlanPage() {
        PageResult<MesProMrpPlanDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(mrpPlanMapper.selectPage(any(MesProMrpPlanPageReqVO.class))).thenReturn(page);
        assertEquals(0, mrpService.getMrpPlanPage(new MesProMrpPlanPageReqVO()).getTotal());
    }

    // ========== confirmMrpPlan（CAS：仅已计算可确认） ==========

    @Test
    public void testConfirmMrpPlan_success() {
        when(mrpPlanMapper.updateByIdAndStatus(eq(PLAN_ID),
                eq(Collections.singletonList(MesProMrpPlanStatusEnum.CALCULATED.getStatus())),
                any(MesProMrpPlanDO.class))).thenReturn(1);

        mrpService.confirmMrpPlan(PLAN_ID);

        // 仅允许 CALCULATED(1) 的来源状态，且写入目标状态为 CONFIRMED(2)
        verify(mrpPlanMapper).updateByIdAndStatus(eq(PLAN_ID),
                eq(Collections.singletonList(MesProMrpPlanStatusEnum.CALCULATED.getStatus())),
                argThat(p -> p.getStatus() != null
                        && p.getStatus().equals(MesProMrpPlanStatusEnum.CONFIRMED.getStatus())));
        // 成功路径不回查
        verify(mrpPlanMapper, never()).selectById(anyLong());
    }

    @Test
    public void testConfirmMrpPlan_notExists() {
        when(mrpPlanMapper.updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class)))
                .thenReturn(0);
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(null);

        assertServiceException(() -> mrpService.confirmMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_EXISTS);
    }

    @Test
    public void testConfirmMrpPlan_notCalculated() {
        when(mrpPlanMapper.updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class)))
                .thenReturn(0);
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> mrpService.confirmMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_CALCULATED);
    }

    // ========== convertMrpPlan（CAS：仅已确认可转；逐行落地草稿工单） ==========

    private MesProMrpResultDO buildConvertLine(Long productId, String plannedQty, LocalDateTime orderDate) {
        return MesProMrpResultDO.builder().id(productId).planId(PLAN_ID).productId(productId)
                .plannedOrderQty(plannedQty == null ? null : new BigDecimal(plannedQty))
                .plannedOrderDate(orderDate).build();
    }

    @Test
    public void testConvertMrpPlan_success_bothTypes() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()));
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Arrays.asList(
                buildConvertLine(1L, "10", PLAN_DATE.minusDays(3)),
                buildConvertLine(2L, "20", PLAN_DATE.minusDays(7)),
                buildConvertLine(3L, "0", PLAN_DATE.minusDays(7)),    // 计划订单量 0：跳过
                buildConvertLine(4L, null, PLAN_DATE.minusDays(7)))); // 计划订单量空：跳过
        // 物料 1 有 BOM（生产建议）；物料 2 无 BOM（采购建议）
        when(productBomService.getProductBomListByItemId(1L)).thenReturn(Collections.singletonList(
                MesMdProductBomDO.builder().id(1L).itemId(1L).bomItemId(9L)
                        .quantity(BigDecimal.ONE).build()));
        mockItemMap(MesMdItemDO.builder().id(1L).code("I001").name("成品A").build(),
                MesMdItemDO.builder().id(2L).code("I002").name("来料B").build());
        when(mrpPlanMapper.updateByIdAndStatus(eq(PLAN_ID),
                eq(Collections.singletonList(MesProMrpPlanStatusEnum.CONFIRMED.getStatus())),
                any(MesProMrpPlanDO.class))).thenReturn(1);

        MesProMrpConvertRespVO convertResult = mrpService.convertMrpPlan(PLAN_ID);

        assertEquals(2, convertResult.getTotalCount());
        assertEquals(1, convertResult.getProduceCount());
        assertEquals(1, convertResult.getPurchaseCount());
        // CAS：仅 CONFIRMED 来源，写入 CONVERTED
        verify(mrpPlanMapper).updateByIdAndStatus(eq(PLAN_ID),
                eq(Collections.singletonList(MesProMrpPlanStatusEnum.CONFIRMED.getStatus())),
                argThat(p -> p.getStatus() != null
                        && p.getStatus().equals(MesProMrpPlanStatusEnum.CONVERTED.getStatus())));
        // 逐行建单：生产型=自行生产、采购型=采购；来源=MRP；来源单据=计划编号
        ArgumentCaptor<MesProWorkOrderSaveReqVO> captor = ArgumentCaptor.forClass(MesProWorkOrderSaveReqVO.class);
        verify(workOrderService, times(2)).createWorkOrder(captor.capture());
        MesProWorkOrderSaveReqVO produceReq = captor.getAllValues().get(0);
        assertEquals("MRP-" + PLAN_ID + "-1", produceReq.getCode());
        assertEquals("成品A", produceReq.getName());
        assertEquals(MesProWorkOrderTypeEnum.SELF.getType(), produceReq.getType());
        assertEquals(MesProWorkOrderSourceTypeEnum.MRP.getType(), produceReq.getOrderSourceType());
        assertEquals("MRP-001", produceReq.getOrderSourceCode());
        assertEquals(1L, produceReq.getProductId());
        assertEquals(0, produceReq.getQuantity().compareTo(new BigDecimal("10")));
        assertEquals(PLAN_DATE.minusDays(3), produceReq.getRequestDate());
        assertTrue(produceReq.getRemark().contains("MRP-001"));
        MesProWorkOrderSaveReqVO purchaseReq = captor.getAllValues().get(1);
        assertEquals("MRP-" + PLAN_ID + "-2", purchaseReq.getCode());
        assertEquals("来料B", purchaseReq.getName());
        assertEquals(MesProWorkOrderTypeEnum.PURCHASE.getType(), purchaseReq.getType());
        assertEquals(MesProWorkOrderSourceTypeEnum.MRP.getType(), purchaseReq.getOrderSourceType());
        assertEquals(2L, purchaseReq.getProductId());
        assertEquals(0, purchaseReq.getQuantity().compareTo(new BigDecimal("20")));
    }

    @Test
    public void testConvertMrpPlan_planNotExists() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(null);

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_EXISTS);

        verify(mrpPlanMapper, never()).updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
    }

    @Test
    public void testConvertMrpPlan_notConfirmed() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_CONFIRMED);

        verify(mrpPlanMapper, never()).updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
    }

    @Test
    public void testConvertMrpPlan_noSuggestion_allZeroNullNegative() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()));
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Arrays.asList(
                buildConvertLine(1L, "0", PLAN_DATE), buildConvertLine(2L, null, PLAN_DATE),
                buildConvertLine(3L, "-5", PLAN_DATE)));

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NO_SUGGESTION);

        // 无可转行：不消耗一次性转换（CAS 未被调用）
        verify(mrpPlanMapper, never()).updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class));
    }

    @Test
    public void testConvertMrpPlan_noSuggestion_emptyResults() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()));
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Collections.emptyList());

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NO_SUGGESTION);

        verify(mrpPlanMapper, never()).updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class));
    }

    @Test
    public void testConvertMrpPlan_casLost_planDeleted() {
        // 前置读已确认通过；CAS 0 行后回查：计划已被删（第二次 selectById 返回 null）
        when(mrpPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()))
                .thenReturn(null);
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Collections.singletonList(
                buildConvertLine(1L, "10", PLAN_DATE)));
        when(mrpPlanMapper.updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class))).thenReturn(0);

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_EXISTS);

        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
    }

    @Test
    public void testConvertMrpPlan_casLost_alreadyConverted() {
        // 竞态：他方已转（回查状态已为 CONVERTED）→ 通用 043
        when(mrpPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()))
                .thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONVERTED.getStatus()));
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Collections.singletonList(
                buildConvertLine(1L, "10", PLAN_DATE)));
        when(mrpPlanMapper.updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class))).thenReturn(0);

        assertServiceException(() -> mrpService.convertMrpPlan(PLAN_ID), PRO_MRP_PLAN_NOT_CONFIRMED);

        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
    }

    @Test
    public void testConvertMrpPlan_requestDateAndNameFallback() {
        when(mrpPlanMapper.selectById(PLAN_ID)).thenReturn(buildPlan(MesProMrpPlanStatusEnum.CONFIRMED.getStatus()));
        // 计划订单日期为空（防御回退到计划日期）；物料主数据缺失（名称回退「物料-{id}」）
        when(mrpResultMapper.selectListByPlanId(PLAN_ID)).thenReturn(Collections.singletonList(
                buildConvertLine(7L, "5", null)));
        when(mrpPlanMapper.updateByIdAndStatus(anyLong(), anyList(), any(MesProMrpPlanDO.class))).thenReturn(1);

        MesProMrpConvertRespVO convertResult = mrpService.convertMrpPlan(PLAN_ID);

        assertEquals(1, convertResult.getTotalCount());
        ArgumentCaptor<MesProWorkOrderSaveReqVO> captor = ArgumentCaptor.forClass(MesProWorkOrderSaveReqVO.class);
        verify(workOrderService).createWorkOrder(captor.capture());
        assertEquals("物料-7", captor.getValue().getName());
        assertEquals(PLAN_DATE, captor.getValue().getRequestDate());
        // 无 BOM 主数据（默认空）→ 采购型
        assertEquals(MesProWorkOrderTypeEnum.PURCHASE.getType(), captor.getValue().getType());
    }

}

