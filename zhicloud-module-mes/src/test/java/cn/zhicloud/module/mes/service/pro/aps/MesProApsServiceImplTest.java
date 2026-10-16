package cn.zhicloud.module.mes.service.pro.aps;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.mysql.pro.aps.MesProApsPlanMapper;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import cn.zhicloud.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsStrategy;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_CANNOT_CANCEL;
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
 *（起止同步平移保持时长、真重叠冲突检测、已取消/已完成不阻断）。
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
                PRO_APS_PLAN_RESCHEDULE_TIME_INVALID);
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

}
