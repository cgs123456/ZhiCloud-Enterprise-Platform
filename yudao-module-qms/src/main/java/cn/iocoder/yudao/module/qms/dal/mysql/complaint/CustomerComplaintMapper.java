package cn.iocoder.yudao.module.qms.dal.mysql.complaint;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 客户投诉 Mapper
 *
 * @author yudao
 */
@Mapper
public interface CustomerComplaintMapper extends BaseMapperX<CustomerComplaintDO> {

    default PageResult<CustomerComplaintDO> selectPage(CustomerComplaintPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CustomerComplaintDO>()
                .likeIfPresent(CustomerComplaintDO::getComplaintNo, reqVO.getComplaintNo())
                .eqIfPresent(CustomerComplaintDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CustomerComplaintDO::getProductId, reqVO.getProductId())
                .eqIfPresent(CustomerComplaintDO::getHandleType, reqVO.getHandleType())
                .eqIfPresent(CustomerComplaintDO::getStatus, reqVO.getStatus())
                .orderByDesc(CustomerComplaintDO::getId));
    }

    default CustomerComplaintDO selectByComplaintNo(String complaintNo) {
        return selectOne(CustomerComplaintDO::getComplaintNo, complaintNo);
    }

}