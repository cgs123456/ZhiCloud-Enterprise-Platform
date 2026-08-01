package cn.iocoder.yudao.module.mes.service.pro.aps.strategy;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanStatusEnum;

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
 * 每次调用基于上下文的可用时间副本计算，不会修改上下文中的原始 Map。
 *
 * @author 芋道源码
 */
public final class ApsAllocationHelper {

    private ApsAllocationHelper() {
    }

    /**
     * 默认优先级（中），工单未配置 priority 时回退使用
     */
    private static final int DEFAULT_PRIORITY = MesProApsPlanPriorityEnum.MEDIUM.getPriority();

    /**
     * 按给定工单顺序进行贪婪分配：为每张工单寻找最早空闲工位时段
     *
     * @param orders  待排产工单（按期望顺序）
     * @param context 排产上下文
     * @return 排产结果
     */
    public static ApsResult allocate(List<MesProWorkOrderDO> orders, ApsContext context) {
        // 基于副本计算，避免污染上下文
        Map<Long, LocalDateTime> available = new HashMap<>(context.getWorkstationAvailableTime());
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
            // 2. 寻找最早可用工位
            MesMdWorkstationDO targetWorkstation = null;
            LocalDateTime earliestStart = null;
            for (MesMdWorkstationDO ws : context.getWorkstations()) {
                LocalDateTime availableTime = available.get(ws.getId());
                if (availableTime == null) {
                    availableTime = context.getStartDate();
                }
                if (availableTime.isBefore(context.getStartDate())) {
                    availableTime = context.getStartDate();
                }
                if (earliestStart == null || availableTime.isBefore(earliestStart)) {
                    earliestStart = availableTime;
                    targetWorkstation = ws;
                }
            }
            if (targetWorkstation == null) {
                skipped.add(workOrder.getId());
                continue;
            }
            // 3. 计算结束时间，超过排产截止时间则跳过（产能不足）
            LocalDateTime plannedEnd = earliestStart.plusHours(durationHours);
            if (plannedEnd.isAfter(context.getEndDate())) {
                skipped.add(workOrder.getId());
                continue;
            }
            // 4. 构建排产计划（priority 使用工单自身优先级；planNo/status 由 Service 统一回填）
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
            // 5. 更新工位可用时间
            available.put(targetWorkstation.getId(), plannedEnd);
        }
        return ApsResult.builder().plans(plans).skippedWorkOrderIds(skipped).build();
    }

    /**
     * 解析工单优先级，空值回退到默认中优先级
     */
    public static Integer resolvePriority(MesProWorkOrderDO workOrder) {
        Integer p = workOrder.getPriority();
        return (p == null) ? DEFAULT_PRIORITY : p;
    }

}