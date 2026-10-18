package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * APS 有限产能分配工具
 *
 * 提取贪婪分配核心逻辑，供 {@link GreedyApsStrategy} 与 {@link GeneticApsStrategy}（解码染色体）复用。
 * 每次调用基于上下文的空隙副本计算，不会修改上下文中的原始 Map。
 *
 * 可用性模型（§12.263 空隙建模）：工位容量以「窗口内空闲空隙」表示，
 * 每张工单落于最早可容纳其时长的空隙内；计划只可能落在验证过为空的区间 →
 * 与既有计划重叠不可能、跨窗超排不可能。
 *
 * @author 智云
 */
public final class ApsAllocationHelper {

    private ApsAllocationHelper() {
    }

    /**
     * 默认优先级（中），工单未配置 priority 时回退使用
     */
    private static final int DEFAULT_PRIORITY = MesProApsPlanPriorityEnum.MEDIUM.getPriority();

    /**
     * 按给定工单顺序进行贪婪分配：为每张工单寻找最早可容纳其时长的工位空隙
     *
     * @param orders  待排产工单（按期望顺序）
     * @param context 排产上下文
     * @return 排产结果
     */
    public static ApsResult allocate(List<MesProWorkOrderDO> orders, ApsContext context) {
        // 基于副本计算，避免污染上下文（空隙列表复制为可变列表；ApsTimeRange 不可变，可安全共享）
        Map<Long, List<ApsTimeRange>> freeGaps = copyFreeGaps(context.getWorkstationFreeGaps());
        List<MesProApsPlanDO> plans = new ArrayList<>();
        Set<Long> skipped = new HashSet<>();

        for (MesProWorkOrderDO workOrder : orders) {
            // 1. 估算工序时长
            long durationHours = context.getWorkOrderDurationHours() == null
                    ? 0L
                    : context.getWorkOrderDurationHours().getOrDefault(workOrder.getId(), 0L);
            if (durationHours <= 0) {
                skipped.add(workOrder.getId());
                continue;
            }
            // 2. 寻找「最早可容纳」的空隙：工位按上下文顺序遍历，起点更早者胜
            //    （严格更早才替换——平局取序前者，与旧标量模型同一取胜规则）；
            //    空隙信息缺失（Map 中无该工位）按「本窗不可用」保守处理（服务路径永远全量填充）
            MesMdWorkstationDO targetWorkstation = null;
            LocalDateTime earliestStart = null;
            List<ApsTimeRange> targetGaps = null;
            int targetGapIndex = -1;
            for (MesMdWorkstationDO ws : context.getWorkstations()) {
                List<ApsTimeRange> gaps = freeGaps.get(ws.getId());
                if (gaps == null) {
                    continue;
                }
                for (int i = 0; i < gaps.size(); i++) {
                    ApsTimeRange gap = gaps.get(i);
                    if (gap.canFit(gap.getStart(), durationHours)) {
                        if (earliestStart == null || gap.getStart().isBefore(earliestStart)) {
                            earliestStart = gap.getStart();
                            targetWorkstation = ws;
                            targetGaps = gaps;
                            targetGapIndex = i;
                        }
                        break; // 本工位最早可容纳空隙已定位（后续空隙起点只会更晚）
                    }
                }
            }
            if (targetWorkstation == null) {
                // 产能不足：本窗无任何空隙可容纳该时长
                skipped.add(workOrder.getId());
                continue;
            }
            // 3. 构建排产计划（放置于空隙起点；priority 使用工单自身优先级；planNo/status 由 Service 统一回填）
            LocalDateTime plannedEnd = earliestStart.plusHours(durationHours);
            MesProApsPlanDO plan = MesProApsPlanDO.builder()
                    .workOrderId(workOrder.getId())
                    .productId(workOrder.getProductId())
                    .workstationId(targetWorkstation.getId())
                    .plannedStartTime(earliestStart)
                    .plannedEndTime(plannedEnd)
                    .quantity(workOrder.getQuantity())
                    .priority(resolvePriority(workOrder))
                    .status(MesProApsPlanStatusEnum.DRAFT.getStatus())
                    .build();
            plans.add(plan);
            // 4. 消耗空隙：剩余 [plannedEnd, gapEnd) 回填为新空隙；恰好填满则移除
            ApsTimeRange targetGap = targetGaps.get(targetGapIndex);
            if (plannedEnd.isBefore(targetGap.getEnd())) {
                targetGaps.set(targetGapIndex, new ApsTimeRange(plannedEnd, targetGap.getEnd()));
            } else {
                targetGaps.remove(targetGapIndex);
            }
        }
        return ApsResult.builder().plans(plans).skippedWorkOrderIds(skipped).build();
    }

    /**
     * 复制工位空隙 Map（值列表复制为可变副本，供分配过程消耗）
     */
    private static Map<Long, List<ApsTimeRange>> copyFreeGaps(Map<Long, List<ApsTimeRange>> source) {
        Map<Long, List<ApsTimeRange>> copy = new HashMap<>();
        if (source != null) {
            for (Map.Entry<Long, List<ApsTimeRange>> entry : source.entrySet()) {
                List<ApsTimeRange> gaps = entry.getValue();
                copy.put(entry.getKey(), gaps == null ? new ArrayList<>() : new ArrayList<>(gaps));
            }
        }
        return copy;
    }

    /**
     * 解析工单优先级，空值回退到默认中优先级
     */
    public static Integer resolvePriority(MesProWorkOrderDO workOrder) {
        Integer p = workOrder.getPriority();
        return (p == null) ? DEFAULT_PRIORITY : p;
    }

}
