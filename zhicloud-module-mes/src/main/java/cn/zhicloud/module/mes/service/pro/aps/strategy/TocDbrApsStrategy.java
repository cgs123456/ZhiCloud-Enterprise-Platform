package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.enums.pro.ApsAlgorithmEnum;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TOC-DBR（Drum-Buffer-Rope）约束理论排产策略
 *
 * 简化实现：
 * <ul>
 *   <li>Drum（鼓）：识别瓶颈资源（产能 * 效率最低的工位），按优先级 + 需求日期为工单排程，
 *       瓶颈节奏即工厂节奏。</li>
 *   <li>Buffer（缓冲）：每张工单在瓶颈前设置时间缓冲（默认 1 小时），避免瓶颈饥饿。</li>
 *   <li>Rope（绳）：所有工单投料节奏受限于瓶颈，避免非瓶颈资源过度生产。</li>
 * </ul>
 *
 * @author 智云
 */
@Component
public class TocDbrApsStrategy implements ApsStrategy {

    /**
     * 瓶颈前时间缓冲（小时）
     */
    private static final long BUFFER_HOURS = 1L;

    @Override
    public ApsAlgorithmEnum getAlgorithm() {
        return ApsAlgorithmEnum.TOC_DBR;
    }

    @Override
    public ApsResult schedule(List<MesProWorkOrderDO> orders, ApsContext context) {
        List<MesProApsPlanDO> plans = new ArrayList<>();
        Set<Long> skipped = new HashSet<>();
        if (orders.isEmpty() || context.getWorkstations().isEmpty()) {
            return ApsResult.builder().plans(plans).skippedWorkOrderIds(skipped).build();
        }

        // 1. 识别瓶颈资源：产能 * 效率最低的工位（最紧张）
        MesMdWorkstationDO bottleneck = identifyBottleneck(context.getWorkstations());

        // 2. Drum：按优先级 + 需求日期排序（瓶颈节奏）
        List<MesProWorkOrderDO> drumOrders = new ArrayList<>(orders);
        drumOrders.sort(Comparator
                .comparing(ApsAllocationHelper::resolvePriority, Comparator.naturalOrder())
                .thenComparing(MesProWorkOrderDO::getRequestDate, Comparator.nullsLast(Comparator.naturalOrder())));

        // 3. 按瓶颈节奏排产（所有工单流经瓶颈，受 Rope 限制投料节奏）
        // 瓶颈空闲空隙 + 绳序游标（§12.263 空隙建模）：
        //   候选起点 = max(空隙起点, 绳序游标) + 1h 缓冲
        // 空隙起点即前一占用结束/窗口起点，标量退化工况与旧「下次可用时间 + 缓冲」逐式对齐；
        // 绳序游标单调（不回填更早空隙），保证后序工单不会插到先序工单之前（Drum 顺序）。
        List<ApsTimeRange> bottleneckGaps = new ArrayList<>();
        if (context.getWorkstationFreeGaps() != null
                && context.getWorkstationFreeGaps().get(bottleneck.getId()) != null) {
            bottleneckGaps.addAll(context.getWorkstationFreeGaps().get(bottleneck.getId()));
        }
        LocalDateTime ropeCursor = context.getStartDate();

        for (MesProWorkOrderDO workOrder : drumOrders) {
            long durationHours = context.getWorkOrderDurationHours() == null
                    ? 0L
                    : context.getWorkOrderDurationHours().getOrDefault(workOrder.getId(), 0L);
            if (durationHours <= 0) {
                skipped.add(workOrder.getId());
                continue;
            }
            // Buffer：候选起点先加瓶颈前时间缓冲（1h），再做容纳判定
            LocalDateTime start = null;
            int gapIndex = -1;
            for (int i = 0; i < bottleneckGaps.size(); i++) {
                ApsTimeRange gap = bottleneckGaps.get(i);
                LocalDateTime candidate = gap.getStart().isAfter(ropeCursor) ? gap.getStart() : ropeCursor;
                candidate = candidate.plusHours(BUFFER_HOURS);
                if (gap.canFit(candidate, durationHours)) {
                    start = candidate;
                    gapIndex = i;
                    break;
                }
            }
            // 本窗瓶颈产能不足：跳过且游标不动（后续更短工单仍可在原节奏点尝试）
            if (start == null) {
                skipped.add(workOrder.getId());
                continue;
            }
            LocalDateTime end = start.plusHours(durationHours);
            MesProApsPlanDO plan = MesProApsPlanDO.builder()
                    .workOrderId(workOrder.getId())
                    .productId(workOrder.getProductId())
                    .workstationId(bottleneck.getId())
                    .plannedStartTime(start)
                    .plannedEndTime(end)
                    .quantity(workOrder.getQuantity())
                    .priority(ApsAllocationHelper.resolvePriority(workOrder))
                    .status(MesProApsPlanStatusEnum.DRAFT.getStatus())
                    .build();
            plans.add(plan);
            // Rope：下一张工单的投料节奏受瓶颈约束（游标推进到本计划结束）
            ropeCursor = end;
            // 消耗空隙：剩余 [end, gapEnd) 回填；恰好填满则移除
            ApsTimeRange gap = bottleneckGaps.get(gapIndex);
            if (end.isBefore(gap.getEnd())) {
                bottleneckGaps.set(gapIndex, new ApsTimeRange(end, gap.getEnd()));
            } else {
                bottleneckGaps.remove(gapIndex);
            }
        }
        return ApsResult.builder().plans(plans).skippedWorkOrderIds(skipped).build();
    }

    /**
     * 识别瓶颈资源：产能 * 效率最低（最紧张）的工位
     */
    private MesMdWorkstationDO identifyBottleneck(List<MesMdWorkstationDO> workstations) {
        MesMdWorkstationDO bottleneck = null;
        BigDecimal minThroughput = null;
        for (MesMdWorkstationDO ws : workstations) {
            BigDecimal throughput = resolveThroughput(ws);
            if (minThroughput == null || throughput.compareTo(minThroughput) < 0) {
                minThroughput = throughput;
                bottleneck = ws;
            }
        }
        return bottleneck;
    }

    /**
     * 计算工位吞吐能力：capacity * efficiency，空值按 1.0 处理
     */
    private BigDecimal resolveThroughput(MesMdWorkstationDO ws) {
        BigDecimal capacity = (ws.getCapacity() != null && ws.getCapacity().signum() > 0)
                ? ws.getCapacity() : BigDecimal.ONE;
        BigDecimal efficiency = (ws.getEfficiency() != null && ws.getEfficiency().signum() > 0)
                ? ws.getEfficiency() : BigDecimal.ONE;
        return capacity.multiply(efficiency);
    }
}
