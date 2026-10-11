package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceGroupEmployeeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 考勤组员工关联 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceGroupEmployeeMapper extends BaseMapperX<HrmAttendanceGroupEmployeeDO> {

    default List<HrmAttendanceGroupEmployeeDO> selectListByGroupId(Long groupId) {
        return selectList(HrmAttendanceGroupEmployeeDO::getGroupId, groupId);
    }

    default List<HrmAttendanceGroupEmployeeDO> selectListByEmployeeId(Long employeeId) {
        return selectList(HrmAttendanceGroupEmployeeDO::getEmployeeId, employeeId);
    }

}
