package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryMonthEmployeeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工月度工资 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryMonthEmployeeRecordMapper extends BaseMapperX<HrmSalaryMonthEmployeeRecordDO> {

    default List<HrmSalaryMonthEmployeeRecordDO> selectListByMonthRecordId(Long monthRecordId) {
        return selectList(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId, monthRecordId);
    }

    default HrmSalaryMonthEmployeeRecordDO selectByMonthRecordAndEmployee(Long monthRecordId, Long employeeId) {
        return selectOne(new LambdaQueryWrapperX<HrmSalaryMonthEmployeeRecordDO>()
                .eq(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId, monthRecordId)
                .eq(HrmSalaryMonthEmployeeRecordDO::getEmployeeId, employeeId));
    }

    default Long selectCountByMonthRecordId(Long monthRecordId) {
        return selectCount(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId, monthRecordId);
    }
}
