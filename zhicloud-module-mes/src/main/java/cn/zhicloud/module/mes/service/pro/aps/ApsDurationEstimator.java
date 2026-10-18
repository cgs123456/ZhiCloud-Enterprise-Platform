package cn.zhicloud.module.mes.service.pro.aps;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * APS 工序时长估算工具
 *
 * 自 {@link MesProApsServiceImpl} 抽出（单文件 500 行规则，§12.263 D6），口径与行为不变：
 * <pre>
 *   unitHours     = routeProduct.productionTime（按 timeUnitType 换算为小时）
 *   durationHours = ceil( quantity * unitHours / capacity / efficiency )
 * </pre>
 * 工位 capacity/efficiency 为空时按 1.0 处理；工艺路线 productionTime 为空时回退到旧的
 * "1 件 = 1 小时"简化估算（不阻断排产）。
 *
 * @author 智云
 */
public final class ApsDurationEstimator {

    private ApsDurationEstimator() {
    }

    /**
     * 默认工位产能（件/小时），capacity 为空时回退使用
     */
    private static final BigDecimal DEFAULT_CAPACITY = BigDecimal.ONE;

    /**
     * 默认效率系数，efficiency 为空/非正数时回退使用
     */
    private static final BigDecimal DEFAULT_EFFICIENCY = BigDecimal.ONE;

    /**
     * 旧简化估算的默认单位时长（1 件 = 1 小时）
     */
    private static final BigDecimal LEGACY_UNIT_HOURS = BigDecimal.ONE;

    /**
     * 估算工序时长（小时），基于工艺路线生产用时 + 工位产能/效率
     * <p>
     * 公式：
     * <pre>
     *   unitHours    = routeProduct.productionTime（按 timeUnitType 换算为小时）
     *   capacity     = workstation.capacity（空则 1.0）
     *   efficiency   = workstation.efficiency（空/非正数则 1.0）
     *   durationHours = ceil( quantity * unitHours / capacity / efficiency )
     * </pre>
     * <p>
     * 回退策略（不阻断整体排产）：
     * 1. 工单数量为空或 ≤0 → 返回 0，跳过该工单
     * 2. 工艺路线 productionTime 为空 → 回退到旧的"1 件 = 1 小时"简化估算
     * 3. 工位 capacity/efficiency 为空 → 按 1.0 处理
     *
     * @param workOrder           生产工单
     * @param workstations        可用工位列表（用于取平均产能做估算；具体到工位的差异由分配阶段补偿）
     * @param routeProductService 工艺路线产品服务（读取生产用时）
     * @return 工序时长（小时），返回 0 表示无法排产
     */
    public static long estimateDurationHours(MesProWorkOrderDO workOrder, List<MesMdWorkstationDO> workstations,
                                             MesProRouteProductService routeProductService) {
        // 1. 校验数量
        BigDecimal quantity = workOrder.getQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            return 0L;
        }

        // 2. 读取单位产品工时（小时/件）：优先从工艺路线取，取不到回退到 1 小时/件
        BigDecimal unitHours = resolveUnitHours(workOrder.getProductId(), routeProductService);

        // 3. 取可用工位的平均产能与平均效率（避免单一工位极端值影响整体估算）
        BigDecimal avgCapacity = resolveAvgCapacity(workstations);
        BigDecimal avgEfficiency = resolveAvgEfficiency(workstations);

        // 4. durationHours = ceil( quantity * unitHours / capacity / efficiency )
        BigDecimal duration = quantity.multiply(unitHours)
                .divide(avgCapacity, 4, RoundingMode.HALF_UP)
                .divide(avgEfficiency, 0, RoundingMode.UP);
        // 至少 1 小时
        long hours = duration.longValue();
        return Math.max(hours, 1L);
    }

    /**
     * 解析单位产品工时（小时/件）
     * <p>
     * 从 mes_pro_route_product.productionTime + timeUnitType 换算：
     * - MINUTE → / 60
     * - HOUR   → 直接使用
     * - DAY    → * 24
     * <p>
     * 若产品未配置工艺路线或生产用时为空，回退到 1 小时/件。
     */
    private static BigDecimal resolveUnitHours(Long productId, MesProRouteProductService routeProductService) {
        if (productId == null) {
            return LEGACY_UNIT_HOURS;
        }
        MesProRouteProductDO routeProduct = routeProductService.getRouteProductByItemId(productId);
        if (routeProduct == null || routeProduct.getProductionTime() == null) {
            // 回退到简化估算（不阻断排产）
            return LEGACY_UNIT_HOURS;
        }
        BigDecimal productionTime = routeProduct.getProductionTime();
        String unitType = routeProduct.getTimeUnitType();
        if (unitType == null) {
            return LEGACY_UNIT_HOURS;
        }
        return switch (unitType) {
            case "MINUTE" -> productionTime.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            case "HOUR" -> productionTime;
            case "DAY" -> productionTime.multiply(BigDecimal.valueOf(24));
            default -> LEGACY_UNIT_HOURS;
        };
    }

    /**
     * 解析可用工位的平均产能（件/小时），空值回退到 1.0
     */
    private static BigDecimal resolveAvgCapacity(List<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return DEFAULT_CAPACITY;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (MesMdWorkstationDO ws : workstations) {
            BigDecimal cap = ws.getCapacity();
            if (cap != null && cap.signum() > 0) {
                sum = sum.add(cap);
                count++;
            }
        }
        if (count == 0) {
            return DEFAULT_CAPACITY;
        }
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

    /**
     * 解析可用工位的平均效率系数，空值/非正数回退到 1.0
     */
    private static BigDecimal resolveAvgEfficiency(List<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return DEFAULT_EFFICIENCY;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (MesMdWorkstationDO ws : workstations) {
            BigDecimal eff = ws.getEfficiency();
            if (eff != null && eff.signum() > 0) {
                sum = sum.add(eff);
                count++;
            }
        }
        if (count == 0) {
            return DEFAULT_EFFICIENCY;
        }
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

}
