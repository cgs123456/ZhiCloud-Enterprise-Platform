package cn.zhicloud.module.hr.dal.mysql.recruitment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrJobPostingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrJobPostingMapper extends BaseMapperX<HrJobPostingDO> {

    default PageResult<HrJobPostingDO> selectPage(HrJobPostingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrJobPostingDO>()
                .eqIfPresent(HrJobPostingDO::getPositionId, reqVO.getPositionId())
                .likeIfPresent(HrJobPostingDO::getTitle, reqVO.getTitle())
                .eqIfPresent(HrJobPostingDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrJobPostingDO::getId));
    }

}