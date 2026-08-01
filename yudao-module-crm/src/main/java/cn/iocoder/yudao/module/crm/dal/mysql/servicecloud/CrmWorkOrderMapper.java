package cn.iocoder.yudao.module.crm.dal.mysql.servicecloud;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderPageReqVO;
import cn.iocoder.yudao.module.crm.service.servicecloud.CrmWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 售后工单 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmWorkOrderMapper extends BaseMapperX<CrmWorkOrderDO> {

    default CrmWorkOrderDO selectByNo(String no) {
        return selectOne(CrmWorkOrderDO::getNo, no);
    }

    default PageResult<CrmWorkOrderDO> selectPage(CrmWorkOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmWorkOrderDO>()
                .likeIfPresent(CrmWorkOrderDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmWorkOrderDO::getTitle, pageReqVO.getTitle())
                .eqIfPresent(CrmWorkOrderDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmWorkOrderDO::getWorkOrderType, pageReqVO.getWorkOrderType())
                .eqIfPresent(CrmWorkOrderDO::getPriority, pageReqVO.getPriority())
                .eqIfPresent(CrmWorkOrderDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(CrmWorkOrderDO::getAssigneeUserId, pageReqVO.getAssigneeUserId())
                .betweenIfPresent(CrmWorkOrderDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmWorkOrderDO::getId));
    }

}
