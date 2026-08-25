package cn.zhicloud.module.hr.dal.mysql.leave;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrLeaveBalanceMapper extends BaseMapperX<HrLeaveBalanceDO> {

    default HrLeaveBalanceDO selectByEmployeeAndTypeAndYear(Long employeeId, Long leaveTypeId, Integer year) {
        return selectOne(new LambdaQueryWrapperX<HrLeaveBalanceDO>()
                .eq(HrLeaveBalanceDO::getEmployeeId, employeeId)
                .eq(HrLeaveBalanceDO::getLeaveTypeId, leaveTypeId)
                .eq(HrLeaveBalanceDO::getYear, year));
    }

    default List<HrLeaveBalanceDO> selectListByEmployeeAndYear(Long employeeId, Integer year) {
        return selectList(new LambdaQueryWrapperX<HrLeaveBalanceDO>()
                .eq(HrLeaveBalanceDO::getEmployeeId, employeeId)
                .eq(HrLeaveBalanceDO::getYear, year));
    }

}