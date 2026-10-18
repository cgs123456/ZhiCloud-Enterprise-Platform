package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApsAllocationHelper} 的单元测试（§12.263 空隙分配）
 *
 * @author 智云
 */
public class ApsAllocationHelperTest {

    private static final Long WS_1 = 1L;
    private static final Long WS_2 = 2L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 15, 8, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 15, 20, 0, 0);

    private static MesMdWorkstationDO workstation(Long id) {
        return MesMdWorkstationDO.builder().id(id).build();
    }

    private static MesProWorkOrderDO workOrder(Long id) {
        return MesProWorkOrderDO.builder().id(id).productId(id * 10).build();
    }

    private static ApsTimeRange range(LocalDateTime start, LocalDateTime end) {
        return new ApsTimeRange(start, end);
    }

    private static List<ApsTimeRange> list(ApsTimeRange... ranges) {
        return new ArrayList<>(List.of(ranges));
    }

    private static Map<Long, Long> durations(Object... pairs) {
        Map<Long, Long> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Long) pairs[i], (Long) pairs[i + 1]);
        }
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
    public void testAllocate_prefersEarliestGapAcrossWorkstations() {
        // WS_1 首空隙仅 2h 容不下 3h、次空隙从 +6h 起；WS_2 空隙从 +1h 起 → 应选 WS_2（最早可容纳）
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, START.plusHours(2)), range(START.plusHours(6), END)));
        gaps.put(WS_2, list(range(START.plusHours(1), END)));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1), workstation(WS_2)), gaps, durations(1L, 3L)));

        assertEquals(1, result.getPlans().size());
        MesProApsPlanDO plan = result.getPlans().get(0);
        assertEquals(WS_2, plan.getWorkstationId());
        assertEquals(START.plusHours(1), plan.getPlannedStartTime());
        assertEquals(START.plusHours(4), plan.getPlannedEndTime());
        assertEquals(MesProApsPlanStatusEnum.DRAFT.getStatus(), plan.getStatus());
        assertEquals(MesProApsPlanPriorityEnum.MEDIUM.getPriority(), plan.getPriority());
    }

    @Test
    public void testAllocate_exactFitBoundedGap() {
        // 有界空隙恰为 4h，时长 4h：紧贴边界可容纳
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START.plusHours(2), START.plusHours(6)), range(START.plusHours(10), END)));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1)), gaps, durations(1L, 4L)));

        assertEquals(1, result.getPlans().size());
        assertEquals(START.plusHours(2), result.getPlans().get(0).getPlannedStartTime());
        assertEquals(START.plusHours(6), result.getPlans().get(0).getPlannedEndTime());
    }

    @Test
    public void testAllocate_skipsWhenNoGapFits() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, START.plusHours(2))));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1)), gaps, durations(1L, 3L)));

        assertTrue(result.getPlans().isEmpty());
        assertTrue(result.getSkippedWorkOrderIds().contains(1L));
    }

    @Test
    public void testAllocate_sequentialOrdersConsumeGapBackToBack() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, START.plusHours(4))));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L), workOrder(2L)),
                context(List.of(workstation(WS_1)), gaps, durations(1L, 1L, 2L, 1L)));

        assertEquals(2, result.getPlans().size());
        assertEquals(START, result.getPlans().get(0).getPlannedStartTime());
        assertEquals(START.plusHours(1), result.getPlans().get(1).getPlannedStartTime());
        assertEquals(START.plusHours(2), result.getPlans().get(1).getPlannedEndTime());
    }

    @Test
    public void testAllocate_tieBreaksToFirstWorkstation() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, END)));
        gaps.put(WS_2, list(range(START, END)));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1), workstation(WS_2)), gaps, durations(1L, 2L)));

        assertEquals(1, result.getPlans().size());
        assertEquals(WS_1, result.getPlans().get(0).getWorkstationId());
    }

    @Test
    public void testAllocate_doesNotMutateContextGaps() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, END)));

        ApsContext context = context(List.of(workstation(WS_1)), gaps, durations(1L, 2L));
        ApsAllocationHelper.allocate(List.of(workOrder(1L)), context);

        // 上下文原始空隙不被消耗（分配基于副本）
        List<ApsTimeRange> original = context.getWorkstationFreeGaps().get(WS_1);
        assertEquals(1, original.size());
        assertEquals(START, original.get(0).getStart());
        assertEquals(END, original.get(0).getEnd());
    }

    @Test
    public void testAllocate_missingGapInfoSkipsConservatively() {
        // 上下文缺少该工位空隙信息（服务路径不会发生；防御口径按「本窗不可用」保守处理）
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_2, list(range(START, END)));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1)), gaps, durations(1L, 2L)));

        assertTrue(result.getPlans().isEmpty());
        assertTrue(result.getSkippedWorkOrderIds().contains(1L));
    }

    @Test
    public void testAllocate_invalidDurationSkips() {
        Map<Long, List<ApsTimeRange>> gaps = new HashMap<>();
        gaps.put(WS_1, list(range(START, END)));

        ApsResult result = ApsAllocationHelper.allocate(List.of(workOrder(1L)),
                context(List.of(workstation(WS_1)), gaps, durations(1L, 0L)));

        assertTrue(result.getPlans().isEmpty());
        assertTrue(result.getSkippedWorkOrderIds().contains(1L));
    }

}
