package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceGroupDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HRM 考勤组 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceGroupMapper extends BaseMapperX<HrmAttendanceGroupDO> {

    default PageResult<HrmAttendanceGroupDO> selectPage(PageParam pageParam, String name) {
        return selectPage(pageParam, new LambdaQueryWrapperX<HrmAttendanceGroupDO>()
                .likeIfPresent(HrmAttendanceGroupDO::getName, name)
                .orderByDesc(HrmAttendanceGroupDO::getId));
    }

    default HrmAttendanceGroupDO selectByName(String name) {
        return selectOne(HrmAttendanceGroupDO::getName, name);
    }

    default HrmAttendanceGroupDO selectDefaultGroup() {
        return selectOne(HrmAttendanceGroupDO::getDefaultStatus, true);
    }

}
