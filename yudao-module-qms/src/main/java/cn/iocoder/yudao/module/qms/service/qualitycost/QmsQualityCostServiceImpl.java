package cn.iocoder.yudao.module.qms.service.qualitycost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostSummaryRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostTrendRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.qualitycost.QmsQualityCostDO;
import cn.iocoder.yudao.module.qms.dal.mysql.qualitycost.QmsQualityCostMapper;
import cn.iocoder.yudao.module.qms.enums.qms.QmsQualityCostTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.QUALITY_COST_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.QUALITY_COST_PERIOD_INVALID;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.QUALITY_COST_PERIOD_RANGE_INVALID;

/**
 * QMS 质量成本 Service 实现类（PAIF 模型）
 *
 * @author yudao
 */
@Service
@Validated
public class QmsQualityCostServiceImpl implements QmsQualityCostService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Resource
    private QmsQualityCostMapper qualityCostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQualityCost(QmsQualityCostSaveReqVO createReqVO) {
        validatePeriod(createReqVO.getPeriodMonth());
        QmsQualityCostDO qualityCost = BeanUtils.toBean(createReqVO, QmsQualityCostDO.class);
        qualityCostMapper.insert(qualityCost);
        return qualityCost.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualityCost(QmsQualityCostSaveReqVO updateReqVO) {
        validateQualityCostExists(updateReqVO.getId());
        validatePeriod(updateReqVO.getPeriodMonth());
        QmsQualityCostDO updateObj = BeanUtils.toBean(updateReqVO, QmsQualityCostDO.class);
        qualityCostMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualityCost(Long id) {
        validateQualityCostExists(id);
        qualityCostMapper.deleteById(id);
    }

    private void validateQualityCostExists(Long id) {
        if (qualityCostMapper.selectById(id) == null) {
            throw exception(QUALITY_COST_NOT_EXISTS);
        }
    }

    @Override
    public QmsQualityCostDO getQualityCost(Long id) {
        return qualityCostMapper.selectById(id);
    }

    @Override
    public PageResult<QmsQualityCostDO> getQualityCostPage(QmsQualityCostPageReqVO pageReqVO) {
        return qualityCostMapper.selectPage(pageReqVO);
    }

    @Override
    public QualityCostSummaryRespVO getQualityCostSummary(Integer year, Integer month) {
        validatePeriod(month);
        BigDecimal prevention = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriod(
                QmsQualityCostTypeEnum.PREVENTION.getType(), year, month));
        BigDecimal appraisal = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriod(
                QmsQualityCostTypeEnum.APPRAISAL.getType(), year, month));
        BigDecimal internalFailure = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriod(
                QmsQualityCostTypeEnum.INTERNAL_FAILURE.getType(), year, month));
        BigDecimal externalFailure = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriod(
                QmsQualityCostTypeEnum.EXTERNAL_FAILURE.getType(), year, month));
        return buildSummary(year, month, prevention, appraisal, internalFailure, externalFailure);
    }

    @Override
    public QualityCostTrendRespVO getQualityCostTrend(Integer year) {
        // 1. 查询四类成本在 12 个月的月度聚合（每类一次查询）
        Map<Integer, BigDecimal> preventionMap = toMonthlyMap(
                qualityCostMapper.selectMonthlyAmountByTypeAndYear(QmsQualityCostTypeEnum.PREVENTION.getType(), year));
        Map<Integer, BigDecimal> appraisalMap = toMonthlyMap(
                qualityCostMapper.selectMonthlyAmountByTypeAndYear(QmsQualityCostTypeEnum.APPRAISAL.getType(), year));
        Map<Integer, BigDecimal> internalFailureMap = toMonthlyMap(
                qualityCostMapper.selectMonthlyAmountByTypeAndYear(QmsQualityCostTypeEnum.INTERNAL_FAILURE.getType(), year));
        Map<Integer, BigDecimal> externalFailureMap = toMonthlyMap(
                qualityCostMapper.selectMonthlyAmountByTypeAndYear(QmsQualityCostTypeEnum.EXTERNAL_FAILURE.getType(), year));

        // 2. 拼装 1-12 月趋势
        List<QualityCostTrendRespVO.MonthlyTrendItem> items = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            QualityCostTrendRespVO.MonthlyTrendItem item = new QualityCostTrendRespVO.MonthlyTrendItem();
            item.setPeriodMonth(month);
            BigDecimal p = nullToZero(preventionMap.get(month));
            BigDecimal a = nullToZero(appraisalMap.get(month));
            BigDecimal i = nullToZero(internalFailureMap.get(month));
            BigDecimal e = nullToZero(externalFailureMap.get(month));
            item.setPreventionAmount(p);
            item.setAppraisalAmount(a);
            item.setInternalFailureAmount(i);
            item.setExternalFailureAmount(e);
            item.setTotalAmount(p.add(a).add(i).add(e));
            items.add(item);
        }

        QualityCostTrendRespVO respVO = new QualityCostTrendRespVO();
        respVO.setPeriodYear(year);
        respVO.setItems(items);
        return respVO;
    }

    @Override
    public QualityCostSummaryRespVO getCumulativeQualityCost(Integer year, Integer startMonth, Integer endMonth) {
        validatePeriod(startMonth);
        validatePeriod(endMonth);
        if (startMonth > endMonth) {
            throw exception(QUALITY_COST_PERIOD_RANGE_INVALID);
        }
        BigDecimal prevention = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriodRange(
                QmsQualityCostTypeEnum.PREVENTION.getType(), year, startMonth, endMonth));
        BigDecimal appraisal = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriodRange(
                QmsQualityCostTypeEnum.APPRAISAL.getType(), year, startMonth, endMonth));
        BigDecimal internalFailure = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriodRange(
                QmsQualityCostTypeEnum.INTERNAL_FAILURE.getType(), year, startMonth, endMonth));
        BigDecimal externalFailure = nullToZero(qualityCostMapper.sumAmountByTypeAndPeriodRange(
                QmsQualityCostTypeEnum.EXTERNAL_FAILURE.getType(), year, startMonth, endMonth));
        // 累计汇总：periodMonth 置空表示区间累计
        return buildSummary(year, null, prevention, appraisal, internalFailure, externalFailure);
    }

    /**
     * 校验月份合法性（1-12）
     */
    private void validatePeriod(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw exception(QUALITY_COST_PERIOD_INVALID);
        }
    }

    /**
     * 将 Mapper 返回的月度聚合列表转为 month -> amount 映射
     */
    private Map<Integer, BigDecimal> toMonthlyMap(List<QmsQualityCostMapper.MonthlyAmount> list) {
        Map<Integer, BigDecimal> map = new HashMap<>(12);
        if (list == null) {
            return map;
        }
        for (QmsQualityCostMapper.MonthlyAmount item : list) {
            if (item.getPeriodMonth() != null) {
                map.put(item.getPeriodMonth(), nullToZero(item.getAmount()));
            }
        }
        return map;
    }

    /**
     * 构建汇总 VO：计算总额与四类占比
     */
    private QualityCostSummaryRespVO buildSummary(Integer year, Integer month,
                                                  BigDecimal prevention, BigDecimal appraisal,
                                                  BigDecimal internalFailure, BigDecimal externalFailure) {
        prevention = nullToZero(prevention);
        appraisal = nullToZero(appraisal);
        internalFailure = nullToZero(internalFailure);
        externalFailure = nullToZero(externalFailure);
        BigDecimal total = prevention.add(appraisal).add(internalFailure).add(externalFailure);

        QualityCostSummaryRespVO respVO = new QualityCostSummaryRespVO();
        respVO.setPeriodYear(year);
        respVO.setPeriodMonth(month);
        respVO.setPreventionAmount(prevention);
        respVO.setAppraisalAmount(appraisal);
        respVO.setInternalFailureAmount(internalFailure);
        respVO.setExternalFailureAmount(externalFailure);
        respVO.setTotalAmount(total);
        respVO.setPreventionRatio(ratio(prevention, total));
        respVO.setAppraisalRatio(ratio(appraisal, total));
        respVO.setInternalFailureRatio(ratio(internalFailure, total));
        respVO.setExternalFailureRatio(ratio(externalFailure, total));
        return respVO;
    }

    /**
     * 计算占比（百分比，保留两位小数）；总额为 0 时返回 0
     */
    private BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}