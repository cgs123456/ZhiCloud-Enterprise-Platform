package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceAssessmentDimensionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效考核维度 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceAssessmentDimensionMapper extends BaseMapperX<HrmPerformanceAssessmentDimensionDO> {

    default List<HrmPerformanceAssessmentDimensionDO> selectListByAssessmentId(Long assessmentId) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentDimensionDO>()
                .eq(HrmPerformanceAssessmentDimensionDO::getAssessmentId, assessmentId)
                .orderByAsc(HrmPerformanceAssessmentDimensionDO::getSort)
                .orderByAsc(HrmPerformanceAssessmentDimensionDO::getId));
    }

}
