package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceAssessmentStageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效考核阶段 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceAssessmentStageMapper extends BaseMapperX<HrmPerformanceAssessmentStageDO> {

    default List<HrmPerformanceAssessmentStageDO> selectListByAssessmentId(Long assessmentId) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentStageDO>()
                .eq(HrmPerformanceAssessmentStageDO::getAssessmentId, assessmentId)
                .orderByAsc(HrmPerformanceAssessmentStageDO::getSort)
                .orderByAsc(HrmPerformanceAssessmentStageDO::getId));
    }

}
