package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitPostTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 招聘职位类型 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitPostTypeMapper extends BaseMapperX<HrmRecruitPostTypeDO> {

    default List<HrmRecruitPostTypeDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitPostTypeDO>()
                .eqIfPresent(HrmRecruitPostTypeDO::getStatus, status)
                .orderByAsc(HrmRecruitPostTypeDO::getSort)
                .orderByAsc(HrmRecruitPostTypeDO::getId));
    }

}
