package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.HrmAttendanceMonthRecordPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceMonthRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * HRM 月度考勤汇总 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceMonthRecordMapper extends BaseMapperX<HrmAttendanceMonthRecordDO> {

    default PageResult<HrmAttendanceMonthRecordDO> selectPage(HrmAttendanceMonthRecordPageReqVO reqVO, Collection<Long> employeeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmAttendanceMonthRecordDO>()
                .inIfPresent(HrmAttendanceMonthRecordDO::getEmployeeId, employeeIds)
                .eq(HrmAttendanceMonthRecordDO::getYear, reqVO.getYear())
                .eq(HrmAttendanceMonthRecordDO::getMonth, reqVO.getMonth())
                .eqIfPresent(HrmAttendanceMonthRecordDO::getFullAttendance, reqVO.getFullAttendance())
                .orderByDesc(HrmAttendanceMonthRecordDO::getId));
    }

    default HrmAttendanceMonthRecordDO selectByEmployeeAndMonth(Long employeeId, Integer year, Integer month) {
        return selectOne(new LambdaQueryWrapperX<HrmAttendanceMonthRecordDO>()
                .eq(HrmAttendanceMonthRecordDO::getEmployeeId, employeeId)
                .eq(HrmAttendanceMonthRecordDO::getYear, year)
                .eq(HrmAttendanceMonthRecordDO::getMonth, month));
    }

}
