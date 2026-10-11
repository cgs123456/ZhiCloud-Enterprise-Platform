package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HRM 招聘配置 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitConfigMapper extends BaseMapperX<HrmRecruitConfigDO> {

    default HrmRecruitConfigDO selectByKind(Integer kind) {
        return selectOne(HrmRecruitConfigDO::getKind, kind);
    }

}
