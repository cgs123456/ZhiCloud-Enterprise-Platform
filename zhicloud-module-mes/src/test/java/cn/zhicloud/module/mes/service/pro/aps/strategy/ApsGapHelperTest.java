package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApsGapHelper} 的单元测试（§12.263 空隙构建）
 *
 * @author 智云
 */
public class ApsGapHelperTest {

    private static final Long WS_1 = 1L;
    private static final Long WS_2 = 2L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 15, 8, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 15, 12, 0, 0);

    private static MesMdWorkstationDO workstation(Long id) {
        return MesMdWorkstationDO.builder().id(id).build();
    }

    private static MesProApsPlanDO plan(Long workstationId, LocalDateTime start, LocalDateTime end) {
        return MesProApsPlanDO.builder().workstationId(workstationId)
                .plannedStartTime(start).plannedEndTime(end).build();
    }

    @Test
    public void testBuildFreeGaps_noPlans_givesWholeWindowGap() {
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)), List.of(), START, END);

        assertEquals(1, gaps.get(WS_1).size());
        assertEquals(START, gaps.get(WS_1).get(0).getStart());
        assertEquals(END, gaps.get(WS_1).get(0).getEnd());
    }

    @Test
    public void testBuildFreeGaps_midWindowOccupancy_givesTwoGaps() {
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, START.plusHours(2), START.plusHours(3))),
                START, END);

        assertEquals(2, gaps.get(WS_1).size());
        assertEquals(START, gaps.get(WS_1).get(0).getStart());
        assertEquals(START.plusHours(2), gaps.get(WS_1).get(0).getEnd());
        assertEquals(START.plusHours(3), gaps.get(WS_1).get(1).getStart());
        assertEquals(END, gaps.get(WS_1).get(1).getEnd());
    }

    @Test
    public void testBuildFreeGaps_leftStraddle_clippedToWindowStart() {
        // 左跨计划 [START-2h, START+1h)：裁剪 → [START, START+1h)，空隙从 START+1h 起
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, START.minusHours(2), START.plusHours(1))),
                START, END);

        assertEquals(1, gaps.get(WS_1).size());
        assertEquals(START.plusHours(1), gaps.get(WS_1).get(0).getStart());
        assertEquals(END, gaps.get(WS_1).get(0).getEnd());
    }

    @Test
    public void testBuildFreeGaps_rightStraddle_clippedToWindowEnd() {
        // 右跨计划 [END-1h, END+3h)：裁剪 → [END-1h, END)，早段空隙保留（不再整窗封锁）
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, END.minusHours(1), END.plusHours(3))),
                START, END);

        assertEquals(1, gaps.get(WS_1).size());
        assertEquals(START, gaps.get(WS_1).get(0).getStart());
        assertEquals(END.minusHours(1), gaps.get(WS_1).get(0).getEnd());
    }

    @Test
    public void testBuildFreeGaps_coveringPlan_givesEmptyGaps() {
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, START.minusHours(1), END.plusHours(1))),
                START, END);

        assertEquals(0, gaps.get(WS_1).size());
    }

    @Test
    public void testBuildFreeGaps_overlappingAndAdjacentMerged() {
        // [START, +2h) 与 [+1h, +3h) 重叠、[+3h, END) 与前者相邻：合并后整窗占用
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, START, START.plusHours(2)),
                        plan(WS_1, START.plusHours(1), START.plusHours(3)),
                        plan(WS_1, START.plusHours(3), END)),
                START, END);

        assertEquals(0, gaps.get(WS_1).size());
    }

    @Test
    public void testBuildFreeGaps_workstationIsolationAndFullCoverage() {
        // WS_2 无占用（整窗空隙），WS_1 全占（空列表）；每个工位都有条目
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1), workstation(WS_2)),
                List.of(plan(WS_1, START, END)),
                START, END);

        assertEquals(0, gaps.get(WS_1).size());
        assertEquals(1, gaps.get(WS_2).size());
        assertEquals(START, gaps.get(WS_2).get(0).getStart());
        assertEquals(END, gaps.get(WS_2).get(0).getEnd());
    }

    @Test
    public void testBuildFreeGaps_nullAndOutOfWindowPlans() {
        // 窗口外计划（查询语义之外的防御输入）不产生占用；null 计划列表空值安全
        Map<Long, List<ApsTimeRange>> gaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)),
                List.of(plan(WS_1, START.minusHours(5), START.minusHours(1)),
                        plan(WS_1, END.plusHours(1), END.plusHours(5))),
                START, END);

        assertEquals(1, gaps.get(WS_1).size());
        assertEquals(START, gaps.get(WS_1).get(0).getStart());
        assertEquals(END, gaps.get(WS_1).get(0).getEnd());

        Map<Long, List<ApsTimeRange>> nullGaps = ApsGapHelper.buildFreeGaps(
                List.of(workstation(WS_1)), null, START, END);
        assertEquals(1, nullGaps.get(WS_1).size());
    }

    @Test
    public void testCanFit_boundarySemantics() {
        ApsTimeRange range = new ApsTimeRange(START, END);
        // 紧贴边界（计划结束 == 区间结束）可容纳
        assertTrue(range.canFit(END.minusHours(4), 4));
        // 超出一小时不可容纳
        assertFalse(range.canFit(END.minusHours(4), 5));
        // 起点早于区间起点不可容纳
        assertFalse(range.canFit(START.minusHours(1), 1));
    }

}
