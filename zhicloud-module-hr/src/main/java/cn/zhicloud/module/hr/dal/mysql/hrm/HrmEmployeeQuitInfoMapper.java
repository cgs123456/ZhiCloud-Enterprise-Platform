package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeQuitInfoDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HRM 员工离职信息 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeQuitInfoMapper extends BaseMapperX<HrmEmployeeQuitInfoDO> {

    default HrmEmployeeQuitInfoDO selectByEmployeeId(Long employeeId) {
        return selectOne(HrmEmployeeQuitInfoDO::getEmployeeId, employeeId);
    }

}
