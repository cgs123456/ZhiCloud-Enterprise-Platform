package cn.iocoder.yudao.module.hr.dal.mysql.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrJobPostingPageReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrJobPostingDO;
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