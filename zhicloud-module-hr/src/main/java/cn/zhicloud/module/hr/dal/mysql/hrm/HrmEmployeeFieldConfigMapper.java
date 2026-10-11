package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeFieldConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工字段显示配置 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeFieldConfigMapper extends BaseMapperX<HrmEmployeeFieldConfigDO> {

    default List<HrmEmployeeFieldConfigDO> selectListByKind(Integer kind) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeFieldConfigDO>()
                .eq(HrmEmployeeFieldConfigDO::getKind, kind)
                .orderByAsc(HrmEmployeeFieldConfigDO::getSort)
                .orderByAsc(HrmEmployeeFieldConfigDO::getId));
    }

    default List<HrmEmployeeFieldConfigDO> selectListByKindAndEntryStatus(Integer kind, Integer entryStatus) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeFieldConfigDO>()
                .eq(HrmEmployeeFieldConfigDO::getKind, kind)
                .eq(HrmEmployeeFieldConfigDO::getEntryStatus, entryStatus)
                .orderByAsc(HrmEmployeeFieldConfigDO::getSort)
                .orderByAsc(HrmEmployeeFieldConfigDO::getId));
    }

    default HrmEmployeeFieldConfigDO selectByConfigKey(Integer kind, Integer entryStatus, String name) {
        return selectOne(new LambdaQueryWrapperX<HrmEmployeeFieldConfigDO>()
                .eq(HrmEmployeeFieldConfigDO::getKind, kind)
                .eq(HrmEmployeeFieldConfigDO::getEntryStatus, entryStatus)
                .eq(HrmEmployeeFieldConfigDO::getName, name));
    }

}
