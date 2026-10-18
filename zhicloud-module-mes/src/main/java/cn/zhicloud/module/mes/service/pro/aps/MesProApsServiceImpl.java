package cn.zhicloud.module.mes.service.pro.aps;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.exception.ErrorCode;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.aps.vo.MesProApsGenerateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.aps.vo.MesProApsPlanPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.pro.aps.MesProApsPlanMapper;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanStatusEnum;
import cn.zhicloud.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProductService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import cn.zhicloud.module.mes.enums.pro.ApsAlgorithmEnum;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsContext;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsGapHelper;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsResult;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsStrategy;
import cn.zhicloud.module.mes.service.pro.aps.strategy.ApsTimeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_CANNOT_CANCEL;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_RESCHEDULE_TIME_CONFLICT;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_CAPACITY_EXCEEDED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_DATE_RANGE_INVALID;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_CONFIRMED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_APS_PLAN_NOT_DRAFT;
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
 * 2. 依次为每张工单寻找最早可容纳其时长的工位空隙（窗口内空闲区间，§12.263 空隙建模）
 * 3. 工序时长按 <b>工艺路线生产用时 + 工位产能/效率</b> 精确估算（{@link ApsDurationEstimator}）：
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

        // 4. 读取区间内已有的排产计划，构建工位空闲空隙（§12.263 空隙建模）
        // 空隙 = 窗口 [startDate, endDate) 扣除既有计划占用（裁剪 + 合并）后的空闲区间：
        // ①中段占用不再把早段空闲一并封锁（新计划可落于早段空隙）；
        // ②跨窗占用只封锁被裁进窗口的部分（旧标量模型「工位下次可用时间 = 最晚结束时间」
        //   在右跨时把整窗判不可排——§12.244.5-2 登记的空隙课题，本项收口）。
        // 查询采用「重叠」语义（左跨/右跨/被包含全覆盖）；占位状态按「排除终态」口径
        // （锚定 MesProApsPlanStatusEnum.TERMINAL_STATUS_VALUES）：仅已完成/已取消不占位，
        // 其余状态（含枚举域外的未知值）一律按占位——保守方向，宁可低估空档不超排。
        // 旧「被包含」查询会漏计左跨计划（空隙虚高 → 新计划可能与既有计划重叠）、
        // 且已取消/已完成计划仍占位（§12.240.5 登记、§12.244 收口，状态集收口 §12.251）
        // 一次 IN 查询 + 内存按 workstationId 分组，避免按工位逐个查询的 N+1
        Map<Long, List<ApsTimeRange>> workstationFreeGaps = ApsGapHelper.buildFreeGaps(workstations,
                apsPlanMapper.selectListOverlappingByWorkstationIdsAndTimeRange(
                        workstations.stream().map(MesMdWorkstationDO::getId).collect(Collectors.toList()),
                        reqVO.getStartDate(), reqVO.getEndDate()),
                reqVO.getStartDate(), reqVO.getEndDate());

        // 5. 构建排产上下文 + 预估工单时长
        Map<Long, Long> workOrderDurationHours = new HashMap<>();
        Set<Long> skippedWorkOrders = new HashSet<>();
        for (MesProWorkOrderDO workOrder : workOrders) {
            long durationHours = ApsDurationEstimator.estimateDurationHours(workOrder, workstations, routeProductService);
            workOrderDurationHours.put(workOrder.getId(), durationHours);
            if (durationHours <= 0) {
                skippedWorkOrders.add(workOrder.getId());
            }
        }
        ApsContext apsContext = ApsContext.builder()
                .startDate(reqVO.getStartDate())
                .endDate(reqVO.getEndDate())
                .workstations(workstations)
                .workstationFreeGaps(workstationFreeGaps)
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
        // 4. 工单时段冲突检测：新时段 [newStartTime, newEndTime) 与同工位其他计划重叠时拒绝
        //    查询采用「重叠」语义（左跨/右跨/被包含全覆盖），且已取消/已完成的计划不占用工位资源
        //    上述入参校验与本处冲突拒绝是两条路径两码（§12.257 拆码，勿再共用一个码）：
        //    - 入参非法（newStart 空或 ≥ 计划结束时间）→ PRO_APS_PLAN_RESCHEDULE_TIME_INVALID
        //    - 时段冲突（新窗与同工位他人计划重叠）   → PRO_APS_PLAN_RESCHEDULE_TIME_CONFLICT
        long durationHours = ChronoUnit.HOURS.between(plan.getPlannedStartTime(), plan.getPlannedEndTime());
        LocalDateTime newEndTime = newStartTime.plusHours(durationHours);
        List<MesProApsPlanDO> sameWorkstationPlans = apsPlanMapper.selectListOverlappingByWorkstation(
                plan.getWorkstationId(), newStartTime, newEndTime);
        for (MesProApsPlanDO other : sameWorkstationPlans) {
            // 排除自身，仅校验其他计划是否与新时段重叠
            if (other.getId().equals(planId)) {
                continue;
            }
            if (other.getPlannedStartTime() != null && other.getPlannedEndTime() != null
                    && newStartTime.isBefore(other.getPlannedEndTime())
                    && newEndTime.isAfter(other.getPlannedStartTime())) {
                throw exception(PRO_APS_PLAN_RESCHEDULE_TIME_CONFLICT);
            }
        }
        // 5. 更新计划时段（保持时长不变：开始/结束时间同步平移，避免时长漂移与占用窗虚化）
        apsPlanMapper.updateById(new MesProApsPlanDO().setId(planId)
                .setPlannedStartTime(newStartTime)
                .setPlannedEndTime(newEndTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPlan(Long id) {
        transitionStatus(id, MesProApsPlanStatusEnum.CONFIRMED, PRO_APS_PLAN_NOT_DRAFT,
                MesProApsPlanStatusEnum.DRAFT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPlan(Long id) {
        transitionStatus(id, MesProApsPlanStatusEnum.CANCELLED, PRO_APS_PLAN_CANNOT_CANCEL,
                MesProApsPlanStatusEnum.DRAFT.getStatus(), MesProApsPlanStatusEnum.CONFIRMED.getStatus(),
                MesProApsPlanStatusEnum.EXECUTING.getStatus());
    }

    /**
     * 状态流转统一实现：以「来源状态集合」做条件更新（CAS），并发下只有一个动作能生效
     *
     * <p>affected rows = 0 时回查一次给出精确原因（计划不存在 / 当前状态不允许该动作）。
     * 不用「先查后改」是为了避免两个并发动作各自通过校验后互相覆盖（伪成功）。
     */
    private void transitionStatus(Long id, MesProApsPlanStatusEnum target, ErrorCode errorCode,
                                  Integer... fromStatuses) {
        int updated = apsPlanMapper.updateByIdAndStatus(id, Arrays.asList(fromStatuses),
                MesProApsPlanDO.builder().status(target.getStatus()).build());
        if (updated == 0) {
            MesProApsPlanDO plan = apsPlanMapper.selectById(id);
            if (plan == null) {
                throw exception(PRO_APS_PLAN_NOT_EXISTS);
            }
            throw exception(errorCode, plan.getPlanNo());
        }
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
     * 生成排产计划编号（简化版：APS + 时间戳）
     */
    private String generatePlanNo() {
        return "APS-" + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                .toString().replace("-", "").replace(":", "").replace("T", "");
    }

}
