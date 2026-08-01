package cn.iocoder.yudao.module.mes.service.dv.oeerecord;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.oeerecord.MesDvOeeRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.oeerecord.MesDvOeeRecordMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_OEE_DATE_RANGE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_OEE_MACHINERY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_OEE_PLAN_TIME_INVALID;

/**
 * MES OEE 设备综合效率 Service 实现类
 *
 * OEE 计算公式（ISO 22400-2 完整指标）：
 *
 * <pre>
 * 1. 时间稼动率 TUR (Time Utilization Rate) = RunTime / PlannedProductionTime
 *    等价于 Availability，作为 OEE 的第一个因子
 *
 * 2. 性能效率 PEE (Performance Efficiency) = (IdealCycleTime × TotalProduced) / RunTime
 *    等价于 Performance，作为 OEE 的第二个因子
 *
 * 3. 机械效率 ME (Mechanical Efficiency) = (IdealCycleTime × GoodProduced) / RunTime
 *    区别于 PEE：ME 使用 GoodProduced，反映剔除不良后的纯机械产能
 *
 * 4. 质量率 QR (Quality Rate) = GoodProduced / TotalProduced
 *    等价于 Quality，作为 OEE 的第三个因子
 *
 * 5. OEE = TUR × PEE × QR = Availability × Performance × Quality
 *
 * 说明：本系统 PlannedProductionTime 已扣除计划内停机（如保养/换型），
 *      因此 TUR 直接等价于 Availability，不再单独计算 AU（Availability Utilization）。
 * </pre>
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesDvOeeServiceImpl implements MesDvOeeService {

    /**
     * OEE 计算精度（保留 4 位小数）
     */
    private static final int OEE_SCALE = 4;

    @Resource
    private MesDvOeeRecordMapper oeeRecordMapper;
    @Resource
    private MesDvMachineryService machineryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesDvOeeRecordDO calculateOee(Long machineryId, LocalDateTime startDate, LocalDateTime endDate) {
        // 1. 校验设备存在
        if (machineryService.getMachinery(machineryId) == null) {
            throw exception(DV_OEE_MACHINERY_NOT_EXISTS);
        }
        // 2. 校验时间范围
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw exception(DV_OEE_DATE_RANGE_INVALID);
        }

        // 3. 读取区间内 OEE 记录
        List<MesDvOeeRecordDO> records = oeeRecordMapper.selectListByMachineryAndDateRange(machineryId, startDate, endDate);
        if (records.isEmpty()) {
            return MesDvOeeRecordDO.builder()
                    .machineryId(machineryId)
                    .plannedProductionTime(BigDecimal.ZERO)
                    .runTime(BigDecimal.ZERO)
                    .idealCycleTime(BigDecimal.ZERO)
                    .totalProduced(BigDecimal.ZERO)
                    .goodProduced(BigDecimal.ZERO)
                    .availability(BigDecimal.ZERO)
                    .performance(BigDecimal.ZERO)
                    .quality(BigDecimal.ZERO)
                    .oee(BigDecimal.ZERO)
                    .timeUtilizationRate(BigDecimal.ZERO)
                    .mechanicalEfficiency(BigDecimal.ZERO)
                    .build();
        }

        // 4. 聚合原始数据
        BigDecimal plannedProductionTime = sum(records, MesDvOeeRecordDO::getPlannedProductionTime);
        BigDecimal runTime = sum(records, MesDvOeeRecordDO::getRunTime);
        BigDecimal totalProduced = sum(records, MesDvOeeRecordDO::getTotalProduced);
        BigDecimal goodProduced = sum(records, MesDvOeeRecordDO::getGoodProduced);
        // 理论节拍取最近一条记录（按记录日期倒序后的第一条）
        BigDecimal idealCycleTime = records.get(records.size() - 1).getIdealCycleTime();

        // 5. 计算 ISO 22400-2 完整指标
        // 5.1 时间稼动率 TUR = Availability = RunTime / PlannedProductionTime
        BigDecimal availability = calcAvailability(runTime, plannedProductionTime);
        BigDecimal timeUtilizationRate = availability; // ISO 22400: TUR = Availability
        // 5.2 性能效率 PEE = (IdealCycleTime × TotalProduced) / RunTime
        BigDecimal performance = calcPerformance(totalProduced, idealCycleTime, runTime);
        // 5.3 机械效率 ME = (IdealCycleTime × GoodProduced) / RunTime（区别于 PEE 使用 GoodProduced）
        BigDecimal mechanicalEfficiency = calcMechanicalEfficiency(goodProduced, idealCycleTime, runTime);
        // 5.4 质量率 QR = GoodProduced / TotalProduced
        BigDecimal quality = calcQuality(goodProduced, totalProduced);
        // 5.5 OEE = TUR × PEE × QR
        BigDecimal oee = availability.multiply(performance).multiply(quality).setScale(OEE_SCALE, RoundingMode.HALF_UP);

        return MesDvOeeRecordDO.builder()
                .machineryId(machineryId)
                .plannedProductionTime(plannedProductionTime)
                .runTime(runTime)
                .idealCycleTime(idealCycleTime)
                .totalProduced(totalProduced)
                .goodProduced(goodProduced)
                .availability(availability)
                .performance(performance)
                .quality(quality)
                .oee(oee)
                .timeUtilizationRate(timeUtilizationRate)
                .mechanicalEfficiency(mechanicalEfficiency)
                .build();
    }

    @Override
    public List<MesDvOeeRecordDO> getOeeTrend(Long machineryId, Integer days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days != null && days > 0 ? days : 7);
        return oeeRecordMapper.selectListByMachineryAndDateRange(machineryId, startDate, endDate);
    }

    @Override
    public PageResult<MesDvOeeRecordDO> getOeeRecordPage(MesDvOeeRecordPageReqVO pageReqVO) {
        return oeeRecordMapper.selectPage(pageReqVO);
    }

    // ==================== 工具方法 ====================

    /**
     * 计算可用率 = RunTime / PlannedProductionTime
     */
    private BigDecimal calcAvailability(BigDecimal runTime, BigDecimal plannedProductionTime) {
        if (isNullOrZero(plannedProductionTime)) {
            throw exception(DV_OEE_PLAN_TIME_INVALID);
        }
        if (isNullOrZero(runTime)) {
            return BigDecimal.ZERO;
        }
        return runTime.divide(plannedProductionTime, OEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算表现率 = (TotalProduced * IdealCycleTime) / RunTime
     */
    private BigDecimal calcPerformance(BigDecimal totalProduced, BigDecimal idealCycleTime, BigDecimal runTime) {
        if (isNullOrZero(runTime) || isNullOrZero(totalProduced) || isNullOrZero(idealCycleTime)) {
            return BigDecimal.ZERO;
        }
        BigDecimal numerator = totalProduced.multiply(idealCycleTime);
        return numerator.divide(runTime, OEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算机械效率 ME (ISO 22400-2) = (IdealCycleTime × GoodProduced) / RunTime
     *
     * 区别于 Performance (PEE)：PEE 使用 TotalProduced，ME 使用 GoodProduced，反映剔除不良后的纯机械产能
     */
    private BigDecimal calcMechanicalEfficiency(BigDecimal goodProduced, BigDecimal idealCycleTime, BigDecimal runTime) {
        if (isNullOrZero(runTime) || isNullOrZero(goodProduced) || isNullOrZero(idealCycleTime)) {
            return BigDecimal.ZERO;
        }
        BigDecimal numerator = goodProduced.multiply(idealCycleTime);
        return numerator.divide(runTime, OEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算质量率 = GoodProduced / TotalProduced
     */
    private BigDecimal calcQuality(BigDecimal goodProduced, BigDecimal totalProduced) {
        if (isNullOrZero(totalProduced)) {
            return BigDecimal.ZERO;
        }
        if (isNullOrZero(goodProduced)) {
            return BigDecimal.ZERO;
        }
        return goodProduced.divide(totalProduced, OEE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal sum(List<MesDvOeeRecordDO> records,
                           java.util.function.Function<MesDvOeeRecordDO, BigDecimal> getter) {
        return records.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
