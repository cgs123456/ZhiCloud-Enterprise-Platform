package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceEmployeeInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 员工参保信息 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceEmployeeInfoMapper extends BaseMapperX<HrmInsuranceEmployeeInfoDO> {

    default HrmInsuranceEmployeeInfoDO selectByEmployeeId(Long employeeId) {
        return selectOne(HrmInsuranceEmployeeInfoDO::getEmployeeId, employeeId);
    }

    default List<HrmInsuranceEmployeeInfoDO> selectListByEmployeeIds(Collection<Long> employeeIds) {
        return selectList(HrmInsuranceEmployeeInfoDO::getEmployeeId, employeeIds);
    }

    default Long selectCountBySchemeId(Long schemeId) {
        return selectCount(HrmInsuranceEmployeeInfoDO::getSchemeId, schemeId);
    }

}
