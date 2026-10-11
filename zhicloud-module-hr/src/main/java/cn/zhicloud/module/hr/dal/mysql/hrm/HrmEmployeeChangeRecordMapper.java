package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeChangeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工异动记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeChangeRecordMapper extends BaseMapperX<HrmEmployeeChangeRecordDO> {

    default List<HrmEmployeeChangeRecordDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeChangeRecordDO>()
                .eq(HrmEmployeeChangeRecordDO::getEmployeeId, employeeId)
                .orderByDesc(HrmEmployeeChangeRecordDO::getId)
                .orderByAsc(HrmEmployeeChangeRecordDO::getId));
    }

}
