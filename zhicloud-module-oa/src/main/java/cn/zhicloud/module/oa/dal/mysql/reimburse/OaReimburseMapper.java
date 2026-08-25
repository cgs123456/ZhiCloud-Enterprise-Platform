package cn.zhicloud.module.oa.dal.mysql.reimburse;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 报销单 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaReimburseMapper extends BaseMapperX<OaReimburseDO> {

    default PageResult<OaReimburseDO> selectPage(OaReimbursePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaReimburseDO>()
                .eqIfPresent(OaReimburseDO::getNo, reqVO.getNo())
                .likeIfPresent(OaReimburseDO::getReimburseName, reqVO.getReimburseName())
                .eqIfPresent(OaReimburseDO::getApplicantUserId, reqVO.getApplicantUserId())
                .eqIfPresent(OaReimburseDO::getReimburseType, reqVO.getReimburseType())
                .eqIfPresent(OaReimburseDO::getPaymentStatus, reqVO.getPaymentStatus())
                .eqIfPresent(OaReimburseDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(OaReimburseDO::getReimburseDate, reqVO.getReimburseDate())
                .orderByDesc(OaReimburseDO::getId));
    }

    default OaReimburseDO selectByNo(String no) {
        return selectOne(OaReimburseDO::getNo, no);
    }

}
