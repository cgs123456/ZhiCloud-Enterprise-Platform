package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.HrmAttendanceLeavePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceLeaveDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * HRM 考勤请假 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceLeaveMapper extends BaseMapperX<HrmAttendanceLeaveDO> {

    default PageResult<HrmAttendanceLeaveDO> selectPage(HrmAttendanceLeavePageReqVO reqVO, Collection<Long> employeeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                .inIfPresent(HrmAttendanceLeaveDO::getEmployeeId, employeeIds)
                .inIfPresent(HrmAttendanceLeaveDO::getType, reqVO.getTypes())
                .betweenIfPresent(HrmAttendanceLeaveDO::getStartTime, reqVO.getTimeRange())
                .orderByDesc(HrmAttendanceLeaveDO::getId));
    }

    default List<HrmAttendanceLeaveDO> selectListByEmployeeAndMonth(Long employeeId, LocalDateTime monthStart, LocalDateTime monthEnd) {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                .eq(HrmAttendanceLeaveDO::getEmployeeId, employeeId)
                .ge(HrmAttendanceLeaveDO::getStartTime, monthStart)
                .le(HrmAttendanceLeaveDO::getStartTime, monthEnd)
                .orderByAsc(HrmAttendanceLeaveDO::getStartTime));
    }

    default List<HrmAttendanceLeaveDO> selectListByEmployeeIdsAndMonth(Collection<Long> employeeIds, LocalDateTime monthStart, LocalDateTime monthEnd) {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                .inIfPresent(HrmAttendanceLeaveDO::getEmployeeId, employeeIds)
                .ge(HrmAttendanceLeaveDO::getStartTime, monthStart)
                .le(HrmAttendanceLeaveDO::getStartTime, monthEnd));
    }

}
