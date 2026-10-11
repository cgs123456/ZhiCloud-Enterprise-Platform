package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceMonthEmployeeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工月度社保记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceMonthEmployeeRecordMapper extends BaseMapperX<HrmInsuranceMonthEmployeeRecordDO> {

    default List<HrmInsuranceMonthEmployeeRecordDO> selectListByMonthRecordId(Long monthRecordId) {
        return selectList(HrmInsuranceMonthEmployeeRecordDO::getMonthRecordId, monthRecordId);
    }

    default HrmInsuranceMonthEmployeeRecordDO selectByMonthRecordAndEmployee(Long monthRecordId, Long employeeId) {
        return selectOne(new LambdaQueryWrapperX<HrmInsuranceMonthEmployeeRecordDO>()
                .eq(HrmInsuranceMonthEmployeeRecordDO::getMonthRecordId, monthRecordId)
                .eq(HrmInsuranceMonthEmployeeRecordDO::getEmployeeId, employeeId));
    }

    default Long selectCountByMonthRecordId(Long monthRecordId) {
        return selectCount(HrmInsuranceMonthEmployeeRecordDO::getMonthRecordId, monthRecordId);
    }
}
