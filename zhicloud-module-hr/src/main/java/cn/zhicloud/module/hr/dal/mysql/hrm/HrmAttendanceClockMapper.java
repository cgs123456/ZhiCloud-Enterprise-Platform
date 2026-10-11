package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.HrmAttendanceClockPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceClockDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * HRM 考勤打卡 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceClockMapper extends BaseMapperX<HrmAttendanceClockDO> {

    default PageResult<HrmAttendanceClockDO> selectPage(HrmAttendanceClockPageReqVO reqVO, Collection<Long> employeeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                .inIfPresent(HrmAttendanceClockDO::getEmployeeId, employeeIds)
                .eqIfPresent(HrmAttendanceClockDO::getType, reqVO.getType())
                .likeIfPresent(HrmAttendanceClockDO::getAddress, reqVO.getAddress())
                .inIfPresent(HrmAttendanceClockDO::getSourceType, reqVO.getSourceTypes())
                .betweenIfPresent(HrmAttendanceClockDO::getAttendanceTime, reqVO.getAttendanceTime())
                .orderByDesc(HrmAttendanceClockDO::getClockTime)
                .orderByDesc(HrmAttendanceClockDO::getId));
    }

    default List<HrmAttendanceClockDO> selectListByEmployeeAndDate(Long employeeId, LocalDateTime dayStart, LocalDateTime dayEnd) {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                .eq(HrmAttendanceClockDO::getEmployeeId, employeeId)
                .ge(HrmAttendanceClockDO::getAttendanceTime, dayStart)
                .le(HrmAttendanceClockDO::getAttendanceTime, dayEnd)
                .orderByAsc(HrmAttendanceClockDO::getClockTime));
    }

    default List<HrmAttendanceClockDO> selectListByEmployeeAndMonth(Collection<Long> employeeIds, LocalDateTime monthStart, LocalDateTime monthEnd) {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                .inIfPresent(HrmAttendanceClockDO::getEmployeeId, employeeIds)
                .ge(HrmAttendanceClockDO::getAttendanceTime, monthStart)
                .le(HrmAttendanceClockDO::getAttendanceTime, monthEnd)
                .orderByAsc(HrmAttendanceClockDO::getClockTime));
    }

}
