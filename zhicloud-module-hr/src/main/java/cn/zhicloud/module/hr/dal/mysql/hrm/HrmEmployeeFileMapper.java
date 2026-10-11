package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工档案附件 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeFileMapper extends BaseMapperX<HrmEmployeeFileDO> {

    default List<HrmEmployeeFileDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeFileDO>()
                .eq(HrmEmployeeFileDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeFileDO::getType)
                .orderByAsc(HrmEmployeeFileDO::getId));
    }

}
