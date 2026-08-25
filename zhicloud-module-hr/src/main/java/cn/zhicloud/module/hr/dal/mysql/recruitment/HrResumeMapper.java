package cn.zhicloud.module.hr.dal.mysql.recruitment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrResumePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrResumeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrResumeMapper extends BaseMapperX<HrResumeDO> {

    default PageResult<HrResumeDO> selectPage(HrResumePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrResumeDO>()
                .eqIfPresent(HrResumeDO::getJobPostingId, reqVO.getJobPostingId())
                .likeIfPresent(HrResumeDO::getCandidateName, reqVO.getCandidateName())
                .eqIfPresent(HrResumeDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrResumeDO::getId));
    }

    default List<HrResumeDO> selectListByJobPostingId(Long jobPostingId) {
        return selectList(HrResumeDO::getJobPostingId, jobPostingId);
    }

}