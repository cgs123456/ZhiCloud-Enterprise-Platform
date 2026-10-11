package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeContactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工紧急联系人 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeContactMapper extends BaseMapperX<HrmEmployeeContactDO> {

    default List<HrmEmployeeContactDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeContactDO>()
                .eq(HrmEmployeeContactDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeContactDO::getSort)
                .orderByAsc(HrmEmployeeContactDO::getId));
    }

}
