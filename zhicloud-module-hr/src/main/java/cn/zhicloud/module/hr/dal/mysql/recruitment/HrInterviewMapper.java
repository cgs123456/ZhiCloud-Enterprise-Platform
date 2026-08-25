package cn.zhicloud.module.hr.dal.mysql.recruitment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrInterviewPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrInterviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrInterviewMapper extends BaseMapperX<HrInterviewDO> {

    default PageResult<HrInterviewDO> selectPage(HrInterviewPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrInterviewDO>()
                .eqIfPresent(HrInterviewDO::getResumeId, reqVO.getResumeId())
                .eqIfPresent(HrInterviewDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrInterviewDO::getId));
    }

    default List<HrInterviewDO> selectListByResumeId(Long resumeId) {
        return selectList(HrInterviewDO::getResumeId, resumeId);
    }

}