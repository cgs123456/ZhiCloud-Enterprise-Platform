package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceAssessmentQuotaDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效考核指标 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceAssessmentQuotaMapper extends BaseMapperX<HrmPerformanceAssessmentQuotaDO> {

    default List<HrmPerformanceAssessmentQuotaDO> selectListByAssessmentId(Long assessmentId) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentQuotaDO>()
                .eq(HrmPerformanceAssessmentQuotaDO::getAssessmentId, assessmentId)
                .orderByAsc(HrmPerformanceAssessmentQuotaDO::getSort)
                .orderByAsc(HrmPerformanceAssessmentQuotaDO::getId));
    }

    default List<HrmPerformanceAssessmentQuotaDO> selectListByDimensionId(Long dimensionId) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentQuotaDO>()
                .eq(HrmPerformanceAssessmentQuotaDO::getDimensionId, dimensionId)
                .orderByAsc(HrmPerformanceAssessmentQuotaDO::getSort));
    }

}
