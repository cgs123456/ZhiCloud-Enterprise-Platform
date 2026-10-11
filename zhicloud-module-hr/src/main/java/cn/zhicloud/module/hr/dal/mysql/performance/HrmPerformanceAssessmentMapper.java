package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceAssessmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 员工绩效考核 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceAssessmentMapper extends BaseMapperX<HrmPerformanceAssessmentDO> {

    /**
     * 非归档分页（archive_time 为空）
     */
    default PageResult<HrmPerformanceAssessmentDO> selectPage(HrmPerformanceAssessmentPageReqVO reqVO, Collection<Long> employeeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformanceAssessmentDO>()
                .eqIfPresent(HrmPerformanceAssessmentDO::getPlanId, reqVO.getPlanId())
                .inIfPresent(HrmPerformanceAssessmentDO::getEmployeeId, employeeIds)
                .eqIfPresent(HrmPerformanceAssessmentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrmPerformanceAssessmentDO::getStageType, reqVO.getStageType())
                .isNull(HrmPerformanceAssessmentDO::getArchiveTime)
                .orderByDesc(HrmPerformanceAssessmentDO::getId));
    }

    /**
     * 归档分页（archive_time 非空）
     */
    default PageResult<HrmPerformanceAssessmentDO> selectArchivePage(HrmPerformanceAssessmentPageReqVO reqVO, Collection<Long> employeeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformanceAssessmentDO>()
                .eqIfPresent(HrmPerformanceAssessmentDO::getPlanId, reqVO.getPlanId())
                .inIfPresent(HrmPerformanceAssessmentDO::getEmployeeId, employeeIds)
                .isNotNull(HrmPerformanceAssessmentDO::getArchiveTime)
                .orderByDesc(HrmPerformanceAssessmentDO::getId));
    }

    default List<HrmPerformanceAssessmentDO> selectListByPlanId(Long planId) {
        return selectList(HrmPerformanceAssessmentDO::getPlanId, planId);
    }

    default List<HrmPerformanceAssessmentDO> selectListByEmployeeId(Long employeeId) {
        return selectList(HrmPerformanceAssessmentDO::getEmployeeId, employeeId);
    }

    default HrmPerformanceAssessmentDO selectByPlanAndEmployee(Long planId, Long employeeId) {
        return selectOne(new LambdaQueryWrapperX<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getPlanId, planId)
                .eq(HrmPerformanceAssessmentDO::getEmployeeId, employeeId));
    }

    default Long selectCountByPlanId(Long planId) {
        return selectCount(HrmPerformanceAssessmentDO::getPlanId, planId);
    }

}
