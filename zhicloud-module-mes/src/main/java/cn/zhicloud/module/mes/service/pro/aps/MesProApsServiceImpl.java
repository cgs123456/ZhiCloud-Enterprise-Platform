package cn.zhicloud.module.mes.service.pro.aps;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.aps.vo.MesProApsGenerateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.aps.vo.MesProApsPlanPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.pro.aps.MesProApsPlanMapper;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import cn.zhicloud.module.mes.enums.pro.MesTimeUnitTypeEnum;
import cn.zhicloud.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import cn.zhicloud.module.mes.enums.pro.ApsAlgorithmEnum;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsContext;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsResult;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_CAPACITY_EXCEEDED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_DATE_RANGE_INVALID;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_CONFIRMED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_RESCHEDULE_TIME_INVALID;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_WORK_ORDER_DUPLICATE;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_WORK_ORDER_EMPTY;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_WORK_ORDER_STATUS_INVALID;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_WORKSTATION_EMPTY;

/**
 * MES APS 高级排产 Service 实现类
 *
 * 采用贪婪算法（有限产能排产），P0-12 改造后逻辑：
 * 1. 工单按 <b>自身优先级</b>（高→中→低，空值默认中）+ 需求日期（早→晚）排序
 * 2. 依次为每张工单寻找最早空闲的工位时段
 * 3. 工序时长按 <b>工艺路线生产用时 + 工位产能/效率</b> 精确估算：
 *    <pre>
 *    unitHours    = routeProduct.productionTime（按 timeUnitType 换算为小时）
 *    durationHours = ceil( quantity * unitHours / capacity / efficiency )
 *    </pre>
 *    工位 capacity/efficiency 为空时按 1.0 处理；
 *    工艺路线 productionTime 为空时回退到旧的"1 件 = 1 小时"简化估算并记日志（不阻断排产）。
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class MesProApsServiceImpl implements MesProApsService {

    /**
     * 默认优先级（中），工单未配置 priority 时回退使用
     */
    private static final int DEFAULT_PRIORITY = MesProApsPlanPriorityEnum.MEDIUM.getPriority();

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

    @Resource
    private MesProApsPlanMapper apsPlanMapper;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesProRouteProductService routeProductService;

    /**
     * 全部排产策略（Spring 自动注入所有 {@link ApsStrategy} 实现）
     */
    @Resource
    private java.util.List<ApsStrategy> apsStrategies;
    /**
     * 排产算法配置（GREEDY / TOC_DBR / GENETIC），见 application.yaml: zhicloud.mes.aps.algorithm
     */
    @Value("${zhicloud.mes.aps.algorithm:GREEDY}")
    private String apsAlgorithm;

    @Override
    public List<MesProApsPlanDO> generateSchedule(MesProApsGenerateReqVO reqVO) {
        // 1. 校验工单列表非空
        if (CollUtil.isEmpty(reqVO.getWorkOrderIds())) {
            throw exception(PRO_APS_PLAN_WORK_ORDER_EMPTY);
        }
        // 1.1 校验排产时间范围有效性
        if (reqVO.getStartDate() == null || reqVO.getEndDate() == null
                || !reqVO.getStartDate().isBefore(reqVO.getEndDate())) {
            throw exception(PRO_APS_PLAN_DATE_RANGE_INVALID);
        }
        // 2. 读取工单并按"自身优先级 + 需求日期"排序
        List<MesProWorkOrderDO> workOrders = workOrderService.getWorkOrderList(reqVO.getWorkOrderIds());
        workOrders.sort(Comparator
                .comparing(MesProApsServiceImpl::resolvePriority, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MesProWorkOrderDO::getRequestDate, Comparator.nullsLast(Comparator.naturalOrder())));

        // 2.1 校验工单状态：只有「已确认」状态的工单允许排产
        for (MesProWorkOrderDO workOrder : workOrders) {
            if (!cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum.CONFIRMED.getStatus()
                    .equals(workOrder.getStatus())) {
                throw exception(PRO_APS_PLAN_WORK_ORDER_STATUS_INVALID, workOrder.getCode());
            }
        }

        // 2.2 工单重复排产校验：检查是否已存在 DRAFT/CONFIRMED 状态的排产计划
        List<MesProApsPlanDO> existingPlans = apsPlanMapper.selectListByWorkOrderIds(reqVO.getWorkOrderIds());
        if (CollUtil.isNotEmpty(existingPlans)) {
            Set<Long> duplicateWorkOrderIds = existingPlans.stream()
                    .map(MesProApsPlanDO::getWorkOrderId)
                    .collect(Collectors.toSet());
            // 取第一个重复的工单编号展示
            Long firstDupId = duplicateWorkOrderIds.iterator().next();
            MesProWorkOrderDO firstDup = workOrders.stream()
                    .filter(w -> w.getId().equals(firstDupId))
                    .findFirst().orElse(null);
            throw exception(PRO_APS_PLAN_WORK_ORDER_DUPLICATE,
                    firstDup != null ? firstDup.getCode() : String.valueOf(firstDupId));
        }

        // 3. 读取可用工位（启用状态）
        List<MesMdWorkstationDO> workstations = workstationService.getWorkstationListByStatus(CommonStatusEnum.ENABLE.getStatus());
        if (CollUtil.isEmpty(workstations)) {
            throw exception(PRO_APS_PLAN_WORKSTATION_EMPTY);
        }

        // 4. 读取区间内已有的排产计划，构建工位占用时间线
        // 工位下次可用时间 = 已有计划的最晚结束时间（不早于排产开始时间）
        // 一次 IN 查询 + 内存按 workstationId 分组，避免按工位逐个查询的 N+1
        Map<Long, LocalDateTime> workstationAvailableTime = new HashMap<>();
        List<MesProApsPlanDO> rangePlans = apsPlanMapper.selectListByWorkstationIdsAndTimeRange(
                workstations.stream().map(MesMdWorkstationDO::getId).collect(Collectors.toList()),
                reqVO.getStartDate(), reqVO.getEndDate());
        Map<Long, List<MesProApsPlanDO>> plansByWorkstation = rangePlans.stream()
                .filter(p -> p.getWorkstationId() != null)
                .collect(Collectors.groupingBy(MesProApsPlanDO::getWorkstationId));
        for (MesMdWorkstationDO ws : workstations) {
            LocalDateTime latestEnd = reqVO.getStartDate();
            for (MesProApsPlanDO p : plansByWorkstation.getOrDefault(ws.getId(), Collections.emptyList())) {
                if (p.getPlannedEndTime() != null && p.getPlannedEndTime().isAfter(latestEnd)) {
                    latestEnd = p.getPlannedEndTime();
                }
            }
            workstationAvailableTime.put(ws.getId(), latestEnd);
        }

        // 5. 构建排产上下文 + 预估工单时长
        Map<Long, Long> workOrderDurationHours = new HashMap<>();
        Set<Long> skippedWorkOrders = new HashSet<>();
        for (MesProWorkOrderDO workOrder : workOrders) {
            long durationHours = estimateDurationHours(workOrder, workstations);
            workOrderDurationHours.put(workOrder.getId(), durationHours);
            if (durationHours <= 0) {
                skippedWorkOrders.add(workOrder.getId());
            }
        }
        ApsContext apsContext = ApsContext.builder()
                .startDate(reqVO.getStartDate())
                .endDate(reqVO.getEndDate())
                .workstations(workstations)
                .workstationAvailableTime(workstationAvailableTime)
                .workOrderDurationHours(workOrderDurationHours)
                .build();
        // 6. 按配置选择排产策略执行排产（策略模式：贪婪 / TOC-DBR / 遗传）
        ApsStrategy strategy = selectApsStrategy();
        ApsResult apsResult = strategy.schedule(workOrders, apsContext);
        // 7. 落库：统一回填 planNo + 状态后，经 getSelf() 代理调用事务方法批量持久化
        List<MesProApsPlanDO> result = new ArrayList<>();
        for (MesProApsPlanDO plan : apsResult.getPlans()) {
            plan.setPlanNo(generatePlanNo());
            plan.setStatus(MesProApsPlanStatusEnum.DRAFT.getStatus());
            result.add(plan);
        }
        getSelf().persistPlans(result);
        skippedWorkOrders.addAll(apsResult.getSkippedWorkOrderIds());
        if (!skippedWorkOrders.isEmpty()) {
            log.warn("[generateSchedule][排产算法={}，以下工单因数量无效或产能不足被跳过：{}]",
                    strategy.getAlgorithm().getAlgorithm(), skippedWorkOrders);
        }
        return result;
    }

    /**
     * 批量持久化排产计划（带事务，经 getSelf() 代理调用确保 @Transactional 生效）
     *
     * @param plans 排产计划列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistPlans(List<MesProApsPlanDO> plans) {
        if (CollUtil.isEmpty(plans)) {
            return;
        }
        apsPlanMapper.insertBatch(plans);
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private MesProApsServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

    // ==================== 策略选择 ====================

    /**
     * 按配置选择排产策略
     *
     * @see ApsAlgorithmEnum
     */
    private ApsStrategy selectApsStrategy() {
        ApsAlgorithmEnum algorithm = ApsAlgorithmEnum.parse(apsAlgorithm);
        for (ApsStrategy strategy : apsStrategies) {
            if (strategy.getAlgorithm() == algorithm) {
                return strategy;
            }
        }
        // 未匹配时回退到第一个策略（贪婪），保证排产不中断
        return apsStrategies.get(0);
    }

    @Override
    public List<MesProApsPlanDO> getWorkstationLoad(Long workstationId, LocalDateTime startDate, LocalDateTime endDate) {
        return apsPlanMapper.selectListByWorkstationAndTimeRange(workstationId, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reschedule(Long planId, LocalDateTime newStartTime) {
        // 1. 校验存在
        MesProApsPlanDO plan = validateApsPlanExists(planId);
        // 2. 校验状态：只有已确认状态才能重排产
        if (ObjUtil.notEqual(plan.getStatus(), MesProApsPlanStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_APS_PLAN_NOT_CONFIRMED);
        }
        // 3. 校验新的开始时间必须早于计划结束时间
        if (newStartTime == null || !newStartTime.isBefore(plan.getPlannedEndTime())) {
            throw exception(PRO_APS_PLAN_RESCHEDULE_TIME_INVALID);
        }
        // 4. 工单时段冲突检测：新时段 [newStartTime, plannedEndTime) 与同工位其他计划重叠时拒绝
        long durationHours = ChronoUnit.HOURS.between(plan.getPlannedStartTime(), plan.getPlannedEndTime());
        LocalDateTime newEndTime = newStartTime.plusHours(durationHours);
        List<MesProApsPlanDO> sameWorkstationPlans = apsPlanMapper.selectListByWorkstationAndTimeRange(
                plan.getWorkstationId(), newStartTime, newEndTime);
        for (MesProApsPlanDO other : sameWorkstationPlans) {
            // 排除自身，仅校验其他计划是否与新时段重叠
            if (other.getId().equals(planId)) {
                continue;
            }
            if (other.getPlannedStartTime() != null && other.getPlannedEndTime() != null
                    && newStartTime.isBefore(other.getPlannedEndTime())
                    && newEndTime.isAfter(other.getPlannedStartTime())) {
                throw exception(PRO_APS_PLAN_RESCHEDULE_TIME_INVALID);
            }
        }
        // 5. 更新计划开始时间（保持时长不变）
        apsPlanMapper.updateById(new MesProApsPlanDO().setId(planId)
                .setPlannedStartTime(newStartTime));
    }

    @Override
    public MesProApsPlanDO getApsPlan(Long id) {
        return apsPlanMapper.selectById(id);
    }

    @Override
    public PageResult<MesProApsPlanDO> getApsPlanPage(MesProApsPlanPageReqVO pageReqVO) {
        return apsPlanMapper.selectPage(pageReqVO);
    }

    // ==================== 校验方法 ====================

    private MesProApsPlanDO validateApsPlanExists(Long id) {
        MesProApsPlanDO plan = apsPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(PRO_APS_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    // ==================== 工具方法 ====================

    /**
     * 解析工单优先级，空值回退到默认中优先级
     */
    private static Integer resolvePriority(MesProWorkOrderDO workOrder) {
        Integer p = workOrder.getPriority();
        return (p == null) ? DEFAULT_PRIORITY : p;
    }

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
     * @param workOrder   生产工单
     * @param workstations 可用工位列表（用于取平均产能做估算；具体到工位的差异由贪婪分配阶段补偿）
     * @return 工序时长（小时），返回 0 表示无法排产
     */
    private long estimateDurationHours(MesProWorkOrderDO workOrder, List<MesMdWorkstationDO> workstations) {
        // 1. 校验数量
        BigDecimal quantity = workOrder.getQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            return 0L;
        }

        // 2. 读取单位产品工时（小时/件）：优先从工艺路线取，取不到回退到 1 小时/件
        BigDecimal unitHours = resolveUnitHours(workOrder.getProductId());

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
    private BigDecimal resolveUnitHours(Long productId) {
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
    private BigDecimal resolveAvgCapacity(List<MesMdWorkstationDO> workstations) {
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
    private BigDecimal resolveAvgEfficiency(List<MesMdWorkstationDO> workstations) {
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

    /**
     * 生成排产计划编号（简化版：APS + 时间戳）
     */
    private String generatePlanNo() {
        return "APS-" + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                .toString().replace("-", "").replace(":", "").replace("T", "");
    }

}
