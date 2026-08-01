package cn.iocoder.yudao.module.mes.service.dv.tp;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpKpiMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpRecordMapper;
import cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpRecordResultEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_TP_KPI_NOT_EXISTS;

/**
 * MES TPM KPI 指标 Service 实现类
 *
 * KPI 计算逻辑：
 * 1. MTBF = 总运行时间 / 故障次数
 * 2. MTTR = 总维修时间 / 故障次数
 * 3. OEE 改善值 = 当期 OEE - 上期 OEE
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MesDvTpKpiServiceImpl implements MesDvTpKpiService {

    /**
     * 默认每次故障平均维修时间（小时）
     */
    private static final BigDecimal DEFAULT_REPAIR_TIME_PER_FAILURE = new BigDecimal("2.0");
    /**
     * 默认每月计划停机时间（小时）
     */
    private static final BigDecimal DEFAULT_PLANNED_DOWNTIME_PER_MONTH = new BigDecimal("8.0");

    @Resource
    private MesDvTpKpiMapper tpKpiMapper;
    @Resource
    private MesDvTpRecordMapper tpRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesDvTpKpiDO calculateKpi(Long equipmentId, String period) {
        // 1. 计算周期范围
        LocalDate periodStart = parsePeriodStartDate(period);
        LocalDate periodEnd = parsePeriodEndDate(period);
        // 2. 查询该设备在周期内的执行记录
        List<MesDvTpRecordDO> records = tpRecordMapper.selectListByEquipmentIdAndPeriod(
                equipmentId, periodStart.toString(), periodEnd.toString());
        // 3. 统计故障次数（结果为异常的记录）
        long failureCount = records.stream()
                .filter(r -> MesDvTpRecordResultEnum.ABNORMAL.getResult().equals(r.getResult()))
                .count();
        // 4. 计算总可用时间（小时）：周期天数 * 24
        BigDecimal totalAvailable = new BigDecimal(YearMonth.of(
                Integer.parseInt(period.substring(0, 4)),
                Integer.parseInt(period.substring(4, 6))).lengthOfMonth())
                .multiply(new BigDecimal("24"));
        // 5. 计划停机时间（默认每月 8 小时）
        BigDecimal plannedDowntime = DEFAULT_PLANNED_DOWNTIME_PER_MONTH;
        // 6. 非计划停机时间 = 故障次数 * 平均维修时间
        BigDecimal unplannedDowntime = DEFAULT_REPAIR_TIME_PER_FAILURE.multiply(new BigDecimal(failureCount));
        // 7. 总运行时间 = 总可用时间 - 计划停机 - 非计划停机
        BigDecimal totalRuntime = totalAvailable.subtract(plannedDowntime).subtract(unplannedDowntime);
        if (totalRuntime.compareTo(BigDecimal.ZERO) < 0) {
            totalRuntime = BigDecimal.ZERO;
        }
        // 8. MTBF = 总运行时间 / 故障次数
        BigDecimal mtbf = failureCount > 0
                ? totalRuntime.divide(new BigDecimal(failureCount), 4, RoundingMode.HALF_UP)
                : totalRuntime;
        // 9. MTTR = 总维修时间 / 故障次数
        BigDecimal mttr = failureCount > 0
                ? unplannedDowntime.divide(new BigDecimal(failureCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        // 10. 当期 OEE = 总运行时间 / 总可用时间
        BigDecimal currentOee = totalAvailable.compareTo(BigDecimal.ZERO) > 0
                ? totalRuntime.divide(totalAvailable, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        // 11. 查询上期 KPI，计算 OEE 改善值
        String previousPeriod = getPreviousPeriod(period);
        MesDvTpKpiDO previousKpi = tpKpiMapper.selectByEquipmentAndPeriod(equipmentId, previousPeriod);
        BigDecimal previousOee = BigDecimal.ZERO;
        if (previousKpi != null) {
            // 上期 OEE = (总可用 - 计划停机 - 非计划停机) / 总可用
            BigDecimal prevTotalAvailable = new BigDecimal(YearMonth.of(
                    Integer.parseInt(previousPeriod.substring(0, 4)),
                    Integer.parseInt(previousPeriod.substring(4, 6))).lengthOfMonth())
                    .multiply(new BigDecimal("24"));
            BigDecimal prevRuntime = prevTotalAvailable
                    .subtract(previousKpi.getPlannedDowntime() != null ? previousKpi.getPlannedDowntime() : BigDecimal.ZERO)
                    .subtract(previousKpi.getUnplannedDowntime() != null ? previousKpi.getUnplannedDowntime() : BigDecimal.ZERO);
            if (prevTotalAvailable.compareTo(BigDecimal.ZERO) > 0) {
                previousOee = prevRuntime.divide(prevTotalAvailable, 4, RoundingMode.HALF_UP);
            }
        }
        BigDecimal oeeImprovement = currentOee.subtract(previousOee);
        // 12. 删除旧 KPI（如果存在），插入新 KPI
        MesDvTpKpiDO existKpi = tpKpiMapper.selectByEquipmentAndPeriod(equipmentId, period);
        if (existKpi != null) {
            tpKpiMapper.deleteById(existKpi.getId());
        }
        MesDvTpKpiDO kpi = MesDvTpKpiDO.builder()
                .equipmentId(equipmentId)
                .period(period)
                .mtbf(mtbf)
                .mttr(mttr)
                .oeeImprovement(oeeImprovement)
                .plannedDowntime(plannedDowntime)
                .unplannedDowntime(unplannedDowntime)
                .build();
        tpKpiMapper.insert(kpi);
        log.info("[calculateKpi][equipmentId({}) period({}) failureCount({}) mtbf({}) mttr({}) oeeImprovement({})]",
                equipmentId, period, failureCount, mtbf, mttr, oeeImprovement);
        return kpi;
    }

    @Override
    public MesDvTpKpiDO getTpKpi(Long id) {
        return tpKpiMapper.selectById(id);
    }

    @Override
    public List<MesDvTpKpiDO> getEquipmentKpiHistory(Long equipmentId, List<String> periods) {
        return tpKpiMapper.selectListByEquipmentAndPeriods(equipmentId, periods);
    }

    // ==================== 校验方法 ====================

    /**
     * 校验 KPI 存在
     */
    public MesDvTpKpiDO validateTpKpi(Long id) {
        MesDvTpKpiDO kpi = tpKpiMapper.selectById(id);
        if (kpi == null) {
            throw exception(DV_TP_KPI_NOT_EXISTS);
        }
        return kpi;
    }

    // ==================== 工具方法 ====================

    /**
     * 解析周期为开始日期
     *
     * @param period 周期（yyyyMM）
     * @return 开始日期
     */
    private LocalDate parsePeriodStartDate(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(4, 6));
        return LocalDate.of(year, month, 1);
    }

    /**
     * 解析周期为结束日期
     *
     * @param period 周期（yyyyMM）
     * @return 结束日期
     */
    private LocalDate parsePeriodEndDate(String period) {
        LocalDate start = parsePeriodStartDate(period);
        return start.plusMonths(1).minusDays(1);
    }

    /**
     * 获取上一期周期
     *
     * @param period 当前周期（yyyyMM）
     * @return 上期周期
     */
    private String getPreviousPeriod(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(4, 6));
        YearMonth ym = YearMonth.of(year, month).minusMonths(1);
        return String.format("%04d%02d", ym.getYear(), ym.getMonthValue());
    }

}