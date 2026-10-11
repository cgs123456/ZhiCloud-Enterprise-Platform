package cn.zhicloud.module.hr.dal.mysql.recruit;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitInterviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 招聘面试 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmRecruitInterviewMapper extends BaseMapperX<HrmRecruitInterviewDO> {

    default List<HrmRecruitInterviewDO> selectListByCandidateId(Long candidateId) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitInterviewDO>()
                .eq(HrmRecruitInterviewDO::getCandidateId, candidateId)
                .orderByDesc(HrmRecruitInterviewDO::getStageNumber)
                .orderByDesc(HrmRecruitInterviewDO::getId));
    }

}
