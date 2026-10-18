package cn.zhicloud.module.mes.service.pro.aps;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteCapacityService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * APS 工序时长估算工具
 *
 * 自 {@link MesProApsServiceImpl} 抽出（单文件 500 行规则，§12.263 D6）；产能输入路由化
 * （§12.270 D4-A，ADR 270 D3）：
 * <pre>
 *   unitHours     = routeProduct.productionTime（按 timeUnitType 换算为小时）
 *   durationHours = ceil( quantity * unitHours / capacity / efficiency )
 * </pre>
 * 产能/效率取值优先级：<b>工艺路线产能（mes_pro_route_capacity，capacity > 0）→ 可用工位平均
 * → 1.0</b>；工艺路线 productionTime 为空时回退到旧的 "1 件 = 1 小时" 简化估算（不阻断排产）。
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
     * 估算工序时长（小时），基于工艺路线生产用时 + 工艺路线产能（优先）/ 工位产能（回退）
     * <p>
     * 公式：
     * <pre>
     *   unitHours    = routeProduct.productionTime（按 timeUnitType 换算为小时）
     *   capacity     = 工艺路线产能（capacity > 0）；否则可用工位平均产能（空则 1.0）
     *   efficiency   = 与产能同源（路线效率 / 工位平均效率；空/非正数则 1.0）
     *   durationHours = ceil( quantity * unitHours / capacity / efficiency )
     * </pre>
     * <p>
     * 回退策略（不阻断整体排产）：
     * 1. 工单数量为空或 ≤0 → 返回 0，跳过该工单
     * 2. 工艺路线 productionTime 为空 → 回退到旧的"1 件 = 1 小时"简化估算
     * 3. 工艺路线无产能配置（无行 / capacity ≤ 0）→ 回退工位平均产能/效率（空则 1.0）
     *
     * @param workOrder            生产工单
     * @param workstations         可用工位列表（无路线产能时的回退估算源；具体到工位的差异由分配阶段补偿）
     * @param routeProductService  工艺路线产品服务（读取生产用时与所属路线）
     * @param routeCapacityService 工艺路线产能服务（读取路线产能/效率；无配置返回 null）
     * @return 工序时长（小时），返回 0 表示无法排产
     */
    public static long estimateDurationHours(MesProWorkOrderDO workOrder, List<MesMdWorkstationDO> workstations,
                                             MesProRouteProductService routeProductService,
                                             MesProRouteCapacityService routeCapacityService) {
        // 1. 校验数量
        BigDecimal quantity = workOrder.getQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            return 0L;
        }

        // 2. 读取工艺路线产品（生产用时 + 所属路线；取不到回退 1 小时/件）
        MesProRouteProductDO routeProduct = resolveRouteProduct(workOrder.getProductId(), routeProductService);
        BigDecimal unitHours = resolveUnitHours(routeProduct);

        // 3. 产能/效率取值：路线产能优先（capacity > 0），否则回退工位平均（§12.270 D4-A）
        BigDecimal capacity;
        BigDecimal efficiency;
        MesProRouteCapacityDO routeCapacity = resolveRouteCapacity(routeProduct, routeCapacityService);
        if (routeCapacity != null && routeCapacity.getCapacity() != null
                && routeCapacity.getCapacity().signum() > 0) {
            capacity = routeCapacity.getCapacity();
            efficiency = routeCapacity.getEfficiency() != null && routeCapacity.getEfficiency().signum() > 0
                    ? routeCapacity.getEfficiency() : DEFAULT_EFFICIENCY;
        } else {
            // 取可用工位的平均产能与平均效率（避免单一工位极端值影响整体估算）
            capacity = resolveAvgCapacity(workstations);
            efficiency = resolveAvgEfficiency(workstations);
        }

        // 4. durationHours = ceil( quantity * unitHours / capacity / efficiency )
        BigDecimal duration = quantity.multiply(unitHours)
                .divide(capacity, 4, RoundingMode.HALF_UP)
                .divide(efficiency, 0, RoundingMode.UP);
        // 至少 1 小时
        long hours = duration.longValue();
        return Math.max(hours, 1L);
    }

    /**
     * 解析产品所属工艺路线产品行（生产用时 + 路线编号来源；产品编号为空返回 null）
     */
    private static MesProRouteProductDO resolveRouteProduct(Long productId,
                                                            MesProRouteProductService routeProductService) {
        if (productId == null) {
            return null;
        }
        return routeProductService.getRouteProductByItemId(productId);
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
    private static BigDecimal resolveUnitHours(MesProRouteProductDO routeProduct) {
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
     * 解析工艺路线产能配置（行不存在 / 路线编号为空返回 null，由调用方回退工位平均）
     */
    private static MesProRouteCapacityDO resolveRouteCapacity(MesProRouteProductDO routeProduct,
                                                              MesProRouteCapacityService routeCapacityService) {
        if (routeProduct == null || routeProduct.getRouteId() == null) {
            return null;
        }
        return routeCapacityService.getRouteCapacityByRouteId(routeProduct.getRouteId());
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
