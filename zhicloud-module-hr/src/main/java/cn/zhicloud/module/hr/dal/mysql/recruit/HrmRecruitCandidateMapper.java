package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.HrmRecruitCandidatePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitCandidateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 招聘候选人 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitCandidateMapper extends BaseMapperX<HrmRecruitCandidateDO> {

    default PageResult<HrmRecruitCandidateDO> selectPage(HrmRecruitCandidatePageReqVO reqVO, Collection<Long> postIds, Collection<Long> deptIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmRecruitCandidateDO>()
                .likeIfPresent(HrmRecruitCandidateDO::getName, reqVO.getName())
                .likeIfPresent(HrmRecruitCandidateDO::getMobile, reqVO.getMobile())
                .inIfPresent(HrmRecruitCandidateDO::getPostId, postIds)
                .inIfPresent(HrmRecruitCandidateDO::getDeptId, deptIds)
                .eqIfPresent(HrmRecruitCandidateDO::getChannelId, reqVO.getChannelId())
                .eqIfPresent(HrmRecruitCandidateDO::getOwnerEmployeeId, reqVO.getOwnerEmployeeId())
                .eqIfPresent(HrmRecruitCandidateDO::getEducation, reqVO.getEducation())
                .inIfPresent(HrmRecruitCandidateDO::getStatus, reqVO.getStatuses())
                .orderByDesc(HrmRecruitCandidateDO::getId));
    }

    default List<HrmRecruitCandidateDO> selectListByStatuses(Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitCandidateDO>()
                .inIfPresent(HrmRecruitCandidateDO::getStatus, statuses));
    }

    default List<HrmRecruitCandidateDO> selectListByIds(Collection<Long> ids) {
        return selectList(HrmRecruitCandidateDO::getId, ids);
    }

    default Long selectCountByPostId(Long postId) {
        return selectCount(HrmRecruitCandidateDO::getPostId, postId);
    }

}
