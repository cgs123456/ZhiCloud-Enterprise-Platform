package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalarySlipDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工工资条 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalarySlipMapper extends BaseMapperX<HrmSalarySlipDO> {

    default List<HrmSalarySlipDO> selectListBySendRecordId(Long sendRecordId) {
        return selectList(HrmSalarySlipDO::getSendRecordId, sendRecordId);
    }

    default HrmSalarySlipDO selectBySendRecordAndEmployee(Long sendRecordId, Long employeeId) {
        return selectOne(new LambdaQueryWrapperX<HrmSalarySlipDO>()
                .eq(HrmSalarySlipDO::getSendRecordId, sendRecordId)
                .eq(HrmSalarySlipDO::getEmployeeId, employeeId));
    }
}
