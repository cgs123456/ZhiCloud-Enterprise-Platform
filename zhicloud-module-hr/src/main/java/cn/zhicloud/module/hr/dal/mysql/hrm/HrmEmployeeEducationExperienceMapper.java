package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeEducationExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工教育经历 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeEducationExperienceMapper extends BaseMapperX<HrmEmployeeEducationExperienceDO> {

    default List<HrmEmployeeEducationExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeEducationExperienceDO>()
                .eq(HrmEmployeeEducationExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeEducationExperienceDO::getSort)
                .orderByAsc(HrmEmployeeEducationExperienceDO::getId));
    }

}
