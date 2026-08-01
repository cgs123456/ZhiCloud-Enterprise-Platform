package cn.iocoder.yudao.module.oa.dal.mysql.reimburse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 报销单 Mapper
 *
 * @author yudao
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
