package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeTrainingExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工培训经历 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeTrainingExperienceMapper extends BaseMapperX<HrmEmployeeTrainingExperienceDO> {

    default List<HrmEmployeeTrainingExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeTrainingExperienceDO>()
                .eq(HrmEmployeeTrainingExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeTrainingExperienceDO::getSort)
                .orderByAsc(HrmEmployeeTrainingExperienceDO::getId));
    }

}
