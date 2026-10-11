package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryEmployeeChangeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工调薪记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryEmployeeChangeRecordMapper extends BaseMapperX<HrmSalaryEmployeeChangeRecordDO> {

    default List<HrmSalaryEmployeeChangeRecordDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmSalaryEmployeeChangeRecordDO>()
                .eq(HrmSalaryEmployeeChangeRecordDO::getEmployeeId, employeeId)
                .orderByDesc(HrmSalaryEmployeeChangeRecordDO::getId));
    }
}
