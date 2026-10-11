package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 招聘渠道 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitChannelMapper extends BaseMapperX<HrmRecruitChannelDO> {

    default PageResult<HrmRecruitChannelDO> selectPage(PageParam pageParam, String name) {
        return selectPage(pageParam, new LambdaQueryWrapperX<HrmRecruitChannelDO>()
                .likeIfPresent(HrmRecruitChannelDO::getName, name)
                .orderByAsc(HrmRecruitChannelDO::getSort)
                .orderByAsc(HrmRecruitChannelDO::getId));
    }

    default List<HrmRecruitChannelDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmRecruitChannelDO>()
                .orderByAsc(HrmRecruitChannelDO::getSort)
                .orderByAsc(HrmRecruitChannelDO::getId));
    }

    default HrmRecruitChannelDO selectByName(String name) {
        return selectOne(HrmRecruitChannelDO::getName, name);
    }

    default List<HrmRecruitChannelDO> selectListByIds(java.util.Collection<Long> ids) {
        return selectList(HrmRecruitChannelDO::getId, ids);
    }

}
