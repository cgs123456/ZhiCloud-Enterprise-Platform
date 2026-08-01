package cn.iocoder.yudao.module.qms.dal.mysql.inspectionorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 检验单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InspectionOrderMapper extends BaseMapperX<InspectionOrderDO> {

    default PageResult<InspectionOrderDO> selectPage(InspectionOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<InspectionOrderDO>()
                .likeIfPresent(InspectionOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(InspectionOrderDO::getType, reqVO.getType())
                .eqIfPresent(InspectionOrderDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(InspectionOrderDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(InspectionOrderDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(InspectionOrderDO::getProductId, reqVO.getProductId())
                .eqIfPresent(InspectionOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(InspectionOrderDO::getInspectTime, reqVO.getInspectTime())
                .orderByDesc(InspectionOrderDO::getId));
    }

    default InspectionOrderDO selectByOrderNo(String orderNo) {
        return selectOne(InspectionOrderDO::getOrderNo, orderNo);
    }

}
