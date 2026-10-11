package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryEmployeeInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工薪资档案 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryEmployeeInfoMapper extends BaseMapperX<HrmSalaryEmployeeInfoDO> {

    default HrmSalaryEmployeeInfoDO selectByEmployeeId(Long employeeId) {
        return selectOne(HrmSalaryEmployeeInfoDO::getEmployeeId, employeeId);
    }

    default List<HrmSalaryEmployeeInfoDO> selectListByEmployeeIds(java.util.Collection<Long> employeeIds) {
        return selectList(HrmSalaryEmployeeInfoDO::getEmployeeId, employeeIds);
    }
}
