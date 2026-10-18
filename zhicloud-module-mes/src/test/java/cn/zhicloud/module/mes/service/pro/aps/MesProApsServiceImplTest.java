package cn.zhicloud.module.mes.service.pro.aps;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.pro.aps.vo.MesProApsGenerateReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.pro.aps.MesProApsPlanMapper;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.zhicloud.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsContext;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsResult;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsStrategy;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsTimeRange;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteCapacityService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_CANNOT_CANCEL;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_RESCHEDULE_TIME_CONFLICT;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_CONFIRMED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_DRAFT;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_RESCHEDULE_TIME_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MesProApsServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 *
 * 覆盖业务断点 2：确认/取消状态流转（CAS + 精确错误出口）+ 重排产修正
 *（起止同步平移保持时长、真重叠冲突检测、已取消/已完成不阻断）；
 * A 组第 1 项：generate 工位占用时间线（真重叠 + 状态过滤，§12.244）；
 * C 组第 5 项：generate 工位空闲空隙构建（裁剪/合并/整窗/早段，§12.263）。
 */
@Import(MesProApsServiceImpl.class)
public class MesProApsServiceImplTest extends BaseDbUnitTest {

    private static final Long PLAN_ID = 100L;
    private static final String PLAN_NO = "APS-100";
    private static final Long WORKSTATION_ID = 300L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 15, 8, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 15, 12, 0, 0);

    @MockitoBean
    private MesProApsPlanMapper apsPlanMapper;
    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private MesProRouteProductService routeProductService;
    @MockitoBean
    private MesProRouteCapacityService routeCapacityService;
    @MockitoBean
    private ApsStrategy apsStrategy; // 供 List<ApsStrategy> 注入，本测试不触达排产算法

    @Resource
    private MesProApsServiceImpl apsService;

    private MesProApsPlanDO buildPlan(Integer status) {
        return MesProApsPlanDO.builder()
                .id(PLAN_ID).planNo(PLAN_NO).workstationId(WORKSTATION_ID)
                .plannedStartTime(START).plannedEndTime(END).status(status)
                .build();
    }

    // ==================== 确认（DRAFT -> CONFIRMED） ====================

    @Test
    public void testConfirmPlan_success() {
        when(apsPlanMapper.updateByIdAndStatus(eq(PLAN_ID), anyCollection(), any(MesProApsPlanDO.class)))
                .thenReturn(1);

        apsService.confirmPlan(PLAN_ID);

        verify(apsPlanMapper).updateByIdAndStatus(eq(PLAN_ID),
                argThat((Collection<Integer> from) -> from.size() == 1
                        && from.contains(MesProApsPlanStatusEnum.DRAFT.getStatus())),
                argThat((MesProApsPlanDO update) ->
                        MesProApsPlanStatusEnum.CONFIRMED.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testConfirmPlan_notExists() {
        when(apsPlanMapper.updateByIdAndStatus(anyLong(), anyCollection(), any(MesProApsPlanDO.class)))
                .thenReturn(0);
        when(apsPlanMapper.selectById(PLAN_ID)).thenReturn(null);

        assertServiceException(() -> apsService.confirmPlan(PLAN_ID), PRO_APS_PLAN_NOT_EXISTS);
    }

    @Test
    public void testConfirmPlan_notDraft() {
        when(apsPlanMapper.updateByIdAndStatus(anyLong(), anyCollection(), any(MesProApsPlanDO.class)))
                .thenReturn(0);
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.CONFIRMED.getStatus()));

        assertServiceException(() -> apsService.confirmPlan(PLAN_ID), PRO_APS_PLAN_NOT_DRAFT);
    }

    // ==================== 取消（DRAFT/CONFIRMED/EXECUTING -> CANCELLED） ====================

    @Test
    public void testCancelPlan_success() {
        when(apsPlanMapper.updateByIdAndStatus(eq(PLAN_ID), anyCollection(), any(MesProApsPlanDO.class)))
                .thenReturn(1);

        apsService.cancelPlan(PLAN_ID);

        verify(apsPlanMapper).updateByIdAndStatus(eq(PLAN_ID),
                argThat((Collection<Integer> from) -> from.size() == 3
                        && from.contains(MesProApsPlanStatusEnum.DRAFT.getStatus())
                        && from.contains(MesProApsPlanStatusEnum.CONFIRMED.getStatus())
                        && from.contains(MesProApsPlanStatusEnum.EXECUTING.getStatus())),
                argThat((MesProApsPlanDO update) ->
                        MesProApsPlanStatusEnum.CANCELLED.getStatus().equals(update.getStatus())));
    }

    @Test
    public void testCancelPlan_terminated_rejected() {
        when(apsPlanMapper.updateByIdAndStatus(anyLong(), anyCollection(), any(MesProApsPlanDO.class)))
                .thenReturn(0);
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.CANCELLED.getStatus()));

        assertServiceException(() -> apsService.cancelPlan(PLAN_ID), PRO_APS_PLAN_CANNOT_CANCEL, PLAN_NO);
    }

    // ==================== 重排产 ====================

    @Test
    public void testReschedule_success_movesStartAndEndTogether() {
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.CONFIRMED.getStatus()));
        when(apsPlanMapper.selectListOverlappingByWorkstation(eq(WORKSTATION_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(apsPlanMapper.updateById(any(MesProApsPlanDO.class))).thenReturn(1);

        LocalDateTime newStart = START.minusHours(2); // 06:00（早于原结束 12:00）
        apsService.reschedule(PLAN_ID, newStart);

        ArgumentCaptor<MesProApsPlanDO> captor = ArgumentCaptor.forClass(MesProApsPlanDO.class);
        verify(apsPlanMapper).updateById(captor.capture());
        assertEquals(newStart, captor.getValue().getPlannedStartTime());
        // 时长 4h 保持：结束时间随开始时间同步平移
        assertEquals(newStart.plusHours(4), captor.getValue().getPlannedEndTime());
        // 已改用「重叠」查询，不再用「被包含」查询
        verify(apsPlanMapper, never()).selectListByWorkstationAndTimeRange(any(), any(), any());
    }

    @Test
    public void testReschedule_leftStraddleConflict_rejected() {
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.CONFIRMED.getStatus()));
        // 其他计划 [05:00, 09:00) 与新窗 [06:00, 10:00) 左跨重叠（旧「被包含」查询会漏掉此情形）
        MesProApsPlanDO other = MesProApsPlanDO.builder()
                .id(200L).workstationId(WORKSTATION_ID)
                .plannedStartTime(START.minusHours(3)).plannedEndTime(START.plusHours(1))
                .status(MesProApsPlanStatusEnum.CONFIRMED.getStatus())
                .build();
        when(apsPlanMapper.selectListOverlappingByWorkstation(eq(WORKSTATION_ID), any(), any()))
                .thenReturn(List.of(other));

        assertServiceException(() -> apsService.reschedule(PLAN_ID, START.minusHours(2)),
                PRO_APS_PLAN_RESCHEDULE_TIME_CONFLICT);
        verify(apsPlanMapper, never()).updateById(any(MesProApsPlanDO.class));
    }

    @Test
    public void testReschedule_selfPlanIgnored() {
        MesProApsPlanDO self = buildPlan(MesProApsPlanStatusEnum.CONFIRMED.getStatus());
        when(apsPlanMapper.selectById(PLAN_ID)).thenReturn(self);
        when(apsPlanMapper.selectListOverlappingByWorkstation(eq(WORKSTATION_ID), any(), any()))
                .thenReturn(List.of(self));
        when(apsPlanMapper.updateById(any(MesProApsPlanDO.class))).thenReturn(1);

        apsService.reschedule(PLAN_ID, START.minusHours(1));

        verify(apsPlanMapper).updateById(any(MesProApsPlanDO.class));
    }

    @Test
    public void testReschedule_notConfirmed_rejected() {
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> apsService.reschedule(PLAN_ID, START.minusHours(1)),
                PRO_APS_PLAN_NOT_CONFIRMED);
        verify(apsPlanMapper, never()).updateById(any(MesProApsPlanDO.class));
    }

    @Test
    public void testReschedule_timeNotBeforeEnd_rejected() {
        when(apsPlanMapper.selectById(PLAN_ID))
                .thenReturn(buildPlan(MesProApsPlanStatusEnum.CONFIRMED.getStatus()));

        assertServiceException(() -> apsService.reschedule(PLAN_ID, END),
                PRO_APS_PLAN_RESCHEDULE_TIME_INVALID);
        assertServiceException(() -> apsService.reschedule(PLAN_ID, END.plusHours(1)),
                PRO_APS_PLAN_RESCHEDULE_TIME_INVALID);
        verify(apsPlanMapper, never()).updateById(any(MesProApsPlanDO.class));
    }

    @Test
    public void testReschedule_planNotExists_rejected() {
        when(apsPlanMapper.selectById(PLAN_ID)).thenReturn(null);

        assertServiceException(() -> apsService.reschedule(PLAN_ID, START.minusHours(1)),
                PRO_APS_PLAN_NOT_EXISTS);
    }

    // ==================== 生成排产：工位占用时间线（§12.244 真重叠 + 状态过滤）与空闲空隙（§12.263） ====================

    private MesProApsGenerateReqVO buildGenerateReq(Long workOrderId) {
        MesProApsGenerateReqVO reqVO = new MesProApsGenerateReqVO();
        reqVO.setWorkOrderIds(List.of(workOrderId));
        reqVO.setStartDate(START);
        reqVO.setEndDate(END);
        return reqVO;
    }

    private MesProWorkOrderDO buildGenerateWorkOrder() {
        return MesProWorkOrderDO.builder()
                .id(1000L).code("WO-1")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .quantity(BigDecimal.TEN) // 无工艺路线回退 1h/件 → 时长 10h
                .build();
    }

    /**
     * 执行一次 generate 并捕获传给策略的上下文（供断言工位空闲空隙）
     */
    private ApsContext captureGenerateContext(List<MesProApsPlanDO> existingPlans) {
        MesProWorkOrderDO workOrder = buildGenerateWorkOrder();
        when(workOrderService.getWorkOrderList(any())).thenReturn(new ArrayList<>(List.of(workOrder)));
        when(apsPlanMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationService.getWorkstationListByStatus(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(WORKSTATION_ID).build()));
        when(apsPlanMapper.selectListOverlappingByWorkstationIdsAndTimeRange(any(), any(), any()))
                .thenReturn(existingPlans);
        ArgumentCaptor<ApsContext> contextCaptor = ArgumentCaptor.forClass(ApsContext.class);
        when(apsStrategy.schedule(any(), contextCaptor.capture()))
                .thenReturn(ApsResult.builder().plans(Collections.emptyList())
                        .skippedWorkOrderIds(new HashSet<>()).build());

        apsService.generateSchedule(buildGenerateReq(workOrder.getId()));

        return contextCaptor.getValue();
    }

    @Test
    public void testGenerate_leftStraddlePlanBlocksWholeWindow() {
        // 左跨计划 [START-3h, START+10h)：start < 窗口起点，裁剪后 [START, END) 整窗占用 → 空隙为空（本窗口不可排）
        MesProApsPlanDO leftStraddle = MesProApsPlanDO.builder()
                .id(200L).workstationId(WORKSTATION_ID)
                .plannedStartTime(START.minusHours(3)).plannedEndTime(START.plusHours(10))
                .status(MesProApsPlanStatusEnum.DRAFT.getStatus())
                .build();

        ApsContext context = captureGenerateContext(List.of(leftStraddle));

        assertEquals(Collections.emptyList(), context.getWorkstationFreeGaps().get(WORKSTATION_ID));
        verify(apsPlanMapper).selectListOverlappingByWorkstationIdsAndTimeRange(
                eq(List.of(WORKSTATION_ID)), eq(START), eq(END));
    }

    @Test
    public void testGenerate_noExistingPlansWholeWindowGap() {
        ApsContext context = captureGenerateContext(Collections.emptyList());

        List<ApsTimeRange> gaps = context.getWorkstationFreeGaps().get(WORKSTATION_ID);
        assertEquals(1, gaps.size());
        assertEquals(START, gaps.get(0).getStart());
        assertEquals(END, gaps.get(0).getEnd());
    }

    @Test
    public void testGenerate_rightStraddlePlanLeavesEarlyGap() {
        // 右跨到窗口之外 [START+2h, END+5h)：裁剪后仅封锁 [START+2h, END)，早段 [START, START+2h) 仍为空隙
        //（旧标量模型把可用时间推到窗口外 → 该工位整窗不可排 fail-safe；§12.263 起为空隙语义：早段可利用）
        MesProApsPlanDO spanning = MesProApsPlanDO.builder()
                .id(201L).workstationId(WORKSTATION_ID)
                .plannedStartTime(START.plusHours(2)).plannedEndTime(END.plusHours(5))
                .status(MesProApsPlanStatusEnum.EXECUTING.getStatus())
                .build();

        ApsContext context = captureGenerateContext(List.of(spanning));

        List<ApsTimeRange> gaps = context.getWorkstationFreeGaps().get(WORKSTATION_ID);
        assertEquals(1, gaps.size());
        assertEquals(START, gaps.get(0).getStart());
        assertEquals(START.plusHours(2), gaps.get(0).getEnd());
    }

    @Test
    public void testGenerate_twoAdjacentPlansMergeToEmptyGaps() {
        // 相邻两计划 [START, START+1h) + [START+1h, END)：合并后整窗占用 → 空隙为空
        MesProApsPlanDO first = MesProApsPlanDO.builder()
                .id(202L).workstationId(WORKSTATION_ID)
                .plannedStartTime(START).plannedEndTime(START.plusHours(1))
                .status(MesProApsPlanStatusEnum.DRAFT.getStatus())
                .build();
        MesProApsPlanDO second = MesProApsPlanDO.builder()
                .id(203L).workstationId(WORKSTATION_ID)
                .plannedStartTime(START.plusHours(1)).plannedEndTime(END)
                .status(MesProApsPlanStatusEnum.CONFIRMED.getStatus())
                .build();

        ApsContext context = captureGenerateContext(List.of(first, second));

        assertEquals(Collections.emptyList(), context.getWorkstationFreeGaps().get(WORKSTATION_ID));
    }

    // ==================== 生成排产：时长估算产能输入路由（§12.270 D4-A：路线产能优先 → 工位回退） ====================

    private MesProWorkOrderDO buildGenerateWorkOrderWithProduct() {
        return MesProWorkOrderDO.builder()
                .id(1000L).code("WO-CAP-1")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .productId(2000L)
                .quantity(BigDecimal.TEN)
                .build();
    }

    /**
     * 执行一次 generate 并捕获上下文（产能输入定制版：工艺路线产品 + 路线产能 + 单工位）
     */
    private ApsContext captureGenerateContextWithCapacity(MesProWorkOrderDO workOrder,
                                                          MesProRouteProductDO routeProduct,
                                                          MesProRouteCapacityDO routeCapacity,
                                                          MesMdWorkstationDO workstation) {
        when(workOrderService.getWorkOrderList(any())).thenReturn(new ArrayList<>(List.of(workOrder)));
        when(apsPlanMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationService.getWorkstationListByStatus(any()))
                .thenReturn(workstation != null ? List.of(workstation) : Collections.emptyList());
        when(routeProductService.getRouteProductByItemId(any())).thenReturn(routeProduct);
        when(routeCapacityService.getRouteCapacityByRouteId(any())).thenReturn(routeCapacity);
        ArgumentCaptor<ApsContext> contextCaptor = ArgumentCaptor.forClass(ApsContext.class);
        when(apsStrategy.schedule(any(), contextCaptor.capture()))
                .thenReturn(ApsResult.builder().plans(Collections.emptyList())
                        .skippedWorkOrderIds(new HashSet<>()).build());

        apsService.generateSchedule(buildGenerateReq(workOrder.getId()));

        return contextCaptor.getValue();
    }

    @Test
    public void testGenerate_routeCapacityTakesPriorityOverWorkstation() {
        // 路线产能 2 件/小时：10 件 × 0.5 小时/件 ÷ 2 ÷ 1 = 2.5 → ceil 3 小时（工位产能 100 不参与）
        MesProWorkOrderDO workOrder = buildGenerateWorkOrderWithProduct();
        ApsContext context = captureGenerateContextWithCapacity(workOrder,
                MesProRouteProductDO.builder().routeId(500L)
                        .productionTime(new BigDecimal("0.5")).timeUnitType("HOUR").build(),
                MesProRouteCapacityDO.builder().routeId(500L)
                        .capacity(new BigDecimal("2")).efficiency(BigDecimal.ONE).build(),
                MesMdWorkstationDO.builder().id(WORKSTATION_ID)
                        .capacity(new BigDecimal("100")).efficiency(BigDecimal.ONE).build());

        assertEquals(3L, context.getWorkOrderDurationHours().get(workOrder.getId()).longValue());
    }

    @Test
    public void testGenerate_routeCapacityEfficiencyApplied() {
        // 路线效率 0.5（与产能同源配对）：10 件 × 0.5 ÷ 2 ÷ 0.5 = 5 小时（工位回退口径会得 1 小时）
        MesProWorkOrderDO workOrder = buildGenerateWorkOrderWithProduct();
        ApsContext context = captureGenerateContextWithCapacity(workOrder,
                MesProRouteProductDO.builder().routeId(500L)
                        .productionTime(new BigDecimal("0.5")).timeUnitType("HOUR").build(),
                MesProRouteCapacityDO.builder().routeId(500L)
                        .capacity(new BigDecimal("2")).efficiency(new BigDecimal("0.5")).build(),
                MesMdWorkstationDO.builder().id(WORKSTATION_ID)
                        .capacity(new BigDecimal("100")).efficiency(BigDecimal.ONE).build());

        assertEquals(5L, context.getWorkOrderDurationHours().get(workOrder.getId()).longValue());
    }

    @Test
    public void testGenerate_zeroRouteCapacityFallsBackToWorkstationAverage() {
        // 路线产能 0 = 未配置 → 回退工位平均：10 × 0.5 ÷ 2.5 ÷ 2 = 1 小时
        MesProWorkOrderDO workOrder = buildGenerateWorkOrderWithProduct();
        ApsContext context = captureGenerateContextWithCapacity(workOrder,
                MesProRouteProductDO.builder().routeId(500L)
                        .productionTime(new BigDecimal("0.5")).timeUnitType("HOUR").build(),
                MesProRouteCapacityDO.builder().routeId(500L)
                        .capacity(BigDecimal.ZERO).efficiency(BigDecimal.ONE).build(),
                MesMdWorkstationDO.builder().id(WORKSTATION_ID)
                        .capacity(new BigDecimal("2.5")).efficiency(new BigDecimal("2")).build());

        assertEquals(1L, context.getWorkOrderDurationHours().get(workOrder.getId()).longValue());
    }

    @Test
    public void testGenerate_noRouteProductKeepsLegacyUnitHours() {
        // 无工艺路线产品 → 旧口径：10 件 × 1 小时/件（回退）÷ 工位 5 = 2 小时
        MesProWorkOrderDO workOrder = buildGenerateWorkOrderWithProduct();
        ApsContext context = captureGenerateContextWithCapacity(workOrder,
                null, null,
                MesMdWorkstationDO.builder().id(WORKSTATION_ID)
                        .capacity(new BigDecimal("5")).efficiency(BigDecimal.ONE).build());

        assertEquals(2L, context.getWorkOrderDurationHours().get(workOrder.getId()).longValue());
    }

}
