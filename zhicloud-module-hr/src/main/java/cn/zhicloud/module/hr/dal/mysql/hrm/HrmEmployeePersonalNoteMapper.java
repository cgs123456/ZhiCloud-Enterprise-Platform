package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeePersonalNoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工个人备忘 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeePersonalNoteMapper extends BaseMapperX<HrmEmployeePersonalNoteDO> {

    default List<HrmEmployeePersonalNoteDO> selectListByUserId(Long userId) {
        return selectList(HrmEmployeePersonalNoteDO::getUserId, userId);
    }

}
