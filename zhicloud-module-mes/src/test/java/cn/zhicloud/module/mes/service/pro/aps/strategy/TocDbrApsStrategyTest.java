package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TocDbrApsStrategy} 的单元测试（§12.263 空隙模型下的缓冲与绳序）
 *
 * @author 智云
 */
public class TocDbrApsStrategyTest {

    private final TocDbrApsStrategy strategy = new TocDbrApsStrategy();

    private static final Long WS_1 = 1L;
    private static final Long WS_2 = 2L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 15, 8, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 15, 20, 0, 0);

    private static MesMdWorkstationDO workstation(Long id, String capacity, String efficiency) {
        return MesMdWorkstationDO.builder().id(id)
                .capacity(new BigDecimal(capacity)).efficiency(new BigDecimal(efficiency)).build();
    }

    private static MesProWorkOrderDO workOrder(Long id) {
        return MesProWorkOrderDO.builder().id(id).productId(id * 10).build();
    }

    private static Map<Long, List<ApsTimeRange>> singleGap(Long workstationId,
                                                           LocalDateTime start, LocalDateTime end) {
        Map<Long, List<ApsTimeRange>> map = new HashMap<>();
        map.put(workstationId, new ArrayList<>(List.of(new ApsTimeRange(start, end))));
        return map;
    }

    private static ApsContext context(List<MesMdWorkstationDO> workstations,
                                      Map<Long, List<ApsTimeRange>> gaps, Map<Long, Long> durations) {
        return ApsContext.builder()
                .startDate(START).endDate(END)
                .workstations(workstations)
                .workstationFreeGaps(gaps)
                .workOrderDurationHours(durations)
                .build();
    }

    @Test
    public void testSchedule_bufferAfterGapStart() {
        ApsResult result = strategy.schedule(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1, "1", "1")), singleGap(WS_1, START, END), Map.of(1L, 2L)));

        assertEquals(1, result.getPlans().size());
        MesProApsPlanDO plan = result.getPlans().get(0);
        assertEquals(WS_1, plan.getWorkstationId());
        // 缓冲：空隙起点 + 1h
        assertEquals(START.plusHours(1), plan.getPlannedStartTime());
        assertEquals(START.plusHours(3), plan.getPlannedEndTime());
        assertTrue(result.getSkippedWorkOrderIds().isEmpty());
    }

    @Test
    public void testSchedule_ropeMonotonic_noBackfillEarlierGap() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, new ArrayList<>(List.of(
                new ApsTimeRange(START, START.plusHours(2)),
                new ApsTimeRange(START.plusHours(6), END))));
        Map<Long, Long> durations = new HashMap<>();
        durations.put(1L, 3L);
        durations.put(2L, 1L);

        ApsResult result = strategy.schedule(List.of(workOrder(1L), workOrder(2L)),
                context(List.of(workstation(WS_1, "1", "1")), gaps, durations));

        assertEquals(2, result.getPlans().size());
        // 工单 1：空隙 1 容不下 3h → 落到空隙 2（起点 +1h 缓冲）
        assertEquals(START.plusHours(7), result.getPlans().get(0).getPlannedStartTime());
        // 工单 2：绳序游标已推进到工单 1 结束（再 +1h 缓冲），不回填仍空闲的空隙 1
        assertEquals(START.plusHours(11), result.getPlans().get(1).getPlannedStartTime());
    }

    @Test
    public void testSchedule_skipDoesNotAdvanceCursor() {
        Map<Long, Long> durations = new HashMap<>();
        durations.put(1L, 3L); // 空隙仅 2h：容不下
        durations.put(2L, 1L);

        ApsResult result = strategy.schedule(List.of(workOrder(1L), workOrder(2L)),
                context(List.of(workstation(WS_1, "1", "1")),
                        singleGap(WS_1, START, START.plusHours(2)), durations));

        assertEquals(1, result.getPlans().size());
        assertTrue(result.getSkippedWorkOrderIds().contains(1L));
        // 跳过不推进游标：工单 2 仍在原节奏点（START+1h）尝试并恰好填满空隙
        assertEquals(START.plusHours(1), result.getPlans().get(0).getPlannedStartTime());
    }

    @Test
    public void testSchedule_usesBottleneckWorkstation() {
        MesMdWorkstationDO fast = workstation(WS_1, "2", "1"); // 吞吐 2
        MesMdWorkstationDO slow = workstation(WS_2, "1", "1"); // 吞吐 1 → 瓶颈
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, new ArrayList<>(List.of(new ApsTimeRange(START, END))));
        gaps.put(WS_2, new ArrayList<>(List.of(new ApsTimeRange(START, END))));

        ApsResult result = strategy.schedule(List.of(workOrder(1L)),
                context(List.of(fast, slow), gaps, Map.of(1L, 2L)));

        assertEquals(1, result.getPlans().size());
        assertEquals(WS_2, result.getPlans().get(0).getWorkstationId());
    }

}
