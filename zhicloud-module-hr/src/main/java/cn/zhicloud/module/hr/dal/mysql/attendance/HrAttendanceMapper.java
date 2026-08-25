package cn.zhicloud.module.hr.dal.mysql.attendance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.attendance.vo.HrAttendancePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.attendance.HrAttendanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * HR 考勤记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrAttendanceMapper extends BaseMapperX<HrAttendanceDO> {

    default PageResult<HrAttendanceDO> selectPage(HrAttendancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrAttendanceDO>()
                .eqIfPresent(HrAttendanceDO::getEmployeeId, reqVO.getEmployeeId())
                .eqIfPresent(HrAttendanceDO::getStatus, reqVO.getStatus())
                .geIfPresent(HrAttendanceDO::getAttendanceDate, reqVO.getStartDate())
                .leIfPresent(HrAttendanceDO::getAttendanceDate, reqVO.getEndDate())
                .orderByDesc(HrAttendanceDO::getAttendanceDate));
    }

    default List<HrAttendanceDO> selectListByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<HrAttendanceDO>()
                .eq(HrAttendanceDO::getEmployeeId, employeeId)
                .ge(HrAttendanceDO::getAttendanceDate, startDate)
                .le(HrAttendanceDO::getAttendanceDate, endDate));
    }

    default HrAttendanceDO selectByEmployeeAndDate(Long employeeId, LocalDate attendanceDate) {
        return selectOne(new LambdaQueryWrapperX<HrAttendanceDO>()
                .eq(HrAttendanceDO::getEmployeeId, employeeId)
                .eq(HrAttendanceDO::getAttendanceDate, attendanceDate));
    }

}