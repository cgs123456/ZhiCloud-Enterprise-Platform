package cn.iocoder.yudao.module.wms.dal.mysql.order.asn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS ASN 到货通知单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsAsnOrderMapper extends BaseMapperX<WmsAsnOrderDO> {

    default PageResult<WmsAsnOrderDO> selectPage(WmsAsnOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsAsnOrderDO>()
                .likeIfPresent(WmsAsnOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(WmsAsnOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WmsAsnOrderDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsAsnOrderDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(WmsAsnOrderDO::getDockId, reqVO.getDockId())
                .eqIfPresent(WmsAsnOrderDO::getTransportMode, reqVO.getTransportMode())
                .likeIfPresent(WmsAsnOrderDO::getVehicleNo, reqVO.getVehicleNo())
                .betweenIfPresent(WmsAsnOrderDO::getExpectedArrivalTime, reqVO.getExpectedArrivalTime())
                .orderByDesc(WmsAsnOrderDO::getId));
    }

    default WmsAsnOrderDO selectByNo(String no) {
        return selectOne(WmsAsnOrderDO::getNo, no);
    }

    default Long selectCountByDockId(Long dockId) {
        return selectCount(WmsAsnOrderDO::getDockId, dockId);
    }

}
