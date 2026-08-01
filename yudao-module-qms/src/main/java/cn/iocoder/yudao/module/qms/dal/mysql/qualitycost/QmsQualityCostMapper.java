package cn.iocoder.yudao.module.qms.dal.mysql.qualitycost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.qualitycost.QmsQualityCostDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * QMS 质量成本 Mapper
 *
 * @author yudao
 */
@Mapper
public interface QmsQualityCostMapper extends BaseMapperX<QmsQualityCostDO> {

    default PageResult<QmsQualityCostDO> selectPage(QmsQualityCostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsQualityCostDO>()
                .eqIfPresent(QmsQualityCostDO::getCostType, reqVO.getCostType())
                .likeIfPresent(QmsQualityCostDO::getCostCategory, reqVO.getCostCategory())
                .likeIfPresent(QmsQualityCostDO::getCostItem, reqVO.getCostItem())
                .eqIfPresent(QmsQualityCostDO::getPeriodYear, reqVO.getPeriodYear())
                .eqIfPresent(QmsQualityCostDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(QmsQualityCostDO::getRelatedType, reqVO.getRelatedType())
                .eqIfPresent(QmsQualityCostDO::getRelatedId, reqVO.getRelatedId())
                .orderByDesc(QmsQualityCostDO::getId));
    }

    /**
     * 按成本类型汇总指定年月的金额
     */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM qms_quality_cost WHERE deleted = 0 AND period_year = #{year} AND period_month = #{month} AND cost_type = #{costType}")
    BigDecimal sumAmountByTypeAndPeriod(@Param("costType") String costType,
                                        @Param("year") Integer year,
                                        @Param("month") Integer month);

    /**
     * 按成本类型汇总指定年度累计金额
     */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM qms_quality_cost WHERE deleted = 0 AND period_year = #{year} AND cost_type = #{costType}")
    BigDecimal sumAmountByTypeAndYear(@Param("costType") String costType,
                                      @Param("year") Integer year);

    /**
     * 按成本类型汇总指定年度区间月份累计金额
     */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM qms_quality_cost WHERE deleted = 0 AND period_year = #{year} AND period_month BETWEEN #{startMonth} AND #{endMonth} AND cost_type = #{costType}")
    BigDecimal sumAmountByTypeAndPeriodRange(@Param("costType") String costType,
                                             @Param("year") Integer year,
                                             @Param("startMonth") Integer startMonth,
                                             @Param("endMonth") Integer endMonth);

    /**
     * 按月份汇总指定年度指定类型的金额
     */
    @Select("SELECT period_month, IFNULL(SUM(amount), 0) AS amount FROM qms_quality_cost WHERE deleted = 0 AND period_year = #{year} AND cost_type = #{costType} GROUP BY period_month ORDER BY period_month")
    List<MonthlyAmount> selectMonthlyAmountByTypeAndYear(@Param("costType") String costType,
                                                         @Param("year") Integer year);

    /**
     * 月度金额聚合结果
     */
    @lombok.Data
    class MonthlyAmount {
        private Integer periodMonth;
        private BigDecimal amount;
    }

}