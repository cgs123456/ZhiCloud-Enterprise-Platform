package cn.iocoder.yudao.module.mes.service.pro.aps.strategy;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.ApsAlgorithmEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanStatusEnum;
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
 * @author 芋道源码
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
        LocalDateTime bottleneckAvailable = context.getWorkstationAvailableTime() == null
                ? context.getStartDate()
                : context.getWorkstationAvailableTime().getOrDefault(bottleneck.getId(), context.getStartDate());
        if (bottleneckAvailable.isBefore(context.getStartDate())) {
            bottleneckAvailable = context.getStartDate();
        }

        for (MesProWorkOrderDO workOrder : drumOrders) {
            long durationHours = context.getWorkOrderDurationHours() == null
                    ? 0L
                    : context.getWorkOrderDurationHours().getOrDefault(workOrder.getId(), 0L);
            if (durationHours <= 0) {
                skipped.add(workOrder.getId());
                continue;
            }
            // Buffer：瓶颈前设置时间缓冲，避免瓶颈饥饿
            LocalDateTime start = bottleneckAvailable.plusHours(BUFFER_HOURS);
            if (start.isBefore(context.getStartDate())) {
                start = context.getStartDate();
            }
            LocalDateTime end = start.plusHours(durationHours);
            // 超过排产截止时间，瓶颈产能不足
            if (end.isAfter(context.getEndDate())) {
                skipped.add(workOrder.getId());
                continue;
            }
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
            // Rope：下一张工单的投料节奏受瓶颈约束
            bottleneckAvailable = end;
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
