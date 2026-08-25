package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.performance.vo.HrPerformancePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrPerformanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HR 绩效记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrPerformanceMapper extends BaseMapperX<HrPerformanceDO> {

    default PageResult<HrPerformanceDO> selectPage(HrPerformancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrPerformanceDO>()
                .eqIfPresent(HrPerformanceDO::getEmployeeId, reqVO.getEmployeeId())
                .eqIfPresent(HrPerformanceDO::getPeriod, reqVO.getPeriod())
                .eqIfPresent(HrPerformanceDO::getGrade, reqVO.getGrade())
                .orderByDesc(HrPerformanceDO::getPeriod));
    }

    default List<HrPerformanceDO> selectListByEmployeeIdsAndPeriod(Collection<Long> employeeIds, String period) {
        return selectList(new LambdaQueryWrapperX<HrPerformanceDO>()
                .in(HrPerformanceDO::getEmployeeId, employeeIds)
                .eq(HrPerformanceDO::getPeriod, period));
    }

    default List<HrPerformanceDO> selectListByEmployeeAndPeriod(Long employeeId, String period) {
        return selectList(new LambdaQueryWrapperX<HrPerformanceDO>()
                .eq(HrPerformanceDO::getEmployeeId, employeeId)
                .eq(HrPerformanceDO::getPeriod, period));
    }

}