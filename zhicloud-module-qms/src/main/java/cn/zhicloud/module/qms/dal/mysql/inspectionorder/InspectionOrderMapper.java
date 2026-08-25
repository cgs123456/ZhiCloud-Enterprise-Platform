package cn.zhicloud.module.qms.dal.mysql.inspectionorder;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 检验单 Mapper
 *
 * @author 智云
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

    /**
     * 查询指定业务关联的最新检验单（用于入库前质检卡点）
     *
     * @param bizType 业务类型（枚举 {@link cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum}）
     * @param bizId   业务单据 ID
     * @return 最新检验单，无则 null
     */
    default InspectionOrderDO selectLatestByBiz(String bizType, Long bizId) {
        return selectOne(new LambdaQueryWrapperX<InspectionOrderDO>()
                .eqIfPresent(InspectionOrderDO::getBizType, bizType)
                .eqIfPresent(InspectionOrderDO::getBizId, bizId)
                .orderByDesc(InspectionOrderDO::getId)
                .last("LIMIT 1"));
    }

}
