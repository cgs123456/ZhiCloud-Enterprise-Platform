package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeWorkExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工工作经历 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeWorkExperienceMapper extends BaseMapperX<HrmEmployeeWorkExperienceDO> {

    default List<HrmEmployeeWorkExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeWorkExperienceDO>()
                .eq(HrmEmployeeWorkExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeWorkExperienceDO::getSort)
                .orderByAsc(HrmEmployeeWorkExperienceDO::getId));
    }

}
