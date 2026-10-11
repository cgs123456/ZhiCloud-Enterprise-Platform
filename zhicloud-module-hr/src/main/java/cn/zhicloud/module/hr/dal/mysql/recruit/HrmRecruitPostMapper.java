package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.HrmRecruitPostPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitPostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 招聘职位 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitPostMapper extends BaseMapperX<HrmRecruitPostDO> {

    default PageResult<HrmRecruitPostDO> selectPage(HrmRecruitPostPageReqVO reqVO, Collection<Long> deptIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmRecruitPostDO>()
                .likeIfPresent(HrmRecruitPostDO::getPostName, reqVO.getPostName())
                .inIfPresent(HrmRecruitPostDO::getDeptId, deptIds)
                .eqIfPresent(HrmRecruitPostDO::getPostTypeId, reqVO.getPostTypeId())
                .eqIfPresent(HrmRecruitPostDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrmRecruitPostDO::getJobNature, reqVO.getJobNature())
                .eqIfPresent(HrmRecruitPostDO::getEmergencyLevel, reqVO.getEmergencyLevel())
                .eqIfPresent(HrmRecruitPostDO::getOwnerEmployeeId, reqVO.getOwnerEmployeeId())
                .orderByDesc(HrmRecruitPostDO::getId));
    }

    default List<HrmRecruitPostDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitPostDO>()
                .eqIfPresent(HrmRecruitPostDO::getStatus, status)
                .orderByDesc(HrmRecruitPostDO::getId));
    }

    default List<HrmRecruitPostDO> selectListByIds(Collection<Long> ids) {
        return selectList(HrmRecruitPostDO::getId, ids);
    }

}
