package cn.iocoder.yudao.module.tms.dal.mysql.shipment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentStopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 运单站点 Mapper
 *
 * @author yudao
 */
@Mapper
public interface TmsShipmentStopMapper extends BaseMapperX<TmsShipmentStopDO> {

    default List<TmsShipmentStopDO> selectListByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<TmsShipmentStopDO>()
                .eq(TmsShipmentStopDO::getShipmentId, shipmentId)
                .orderByAsc(TmsShipmentStopDO::getSequenceNo));
    }

    default int deleteByShipmentId(Long shipmentId) {
        return delete(new LambdaQueryWrapperX<TmsShipmentStopDO>()
                .eq(TmsShipmentStopDO::getShipmentId, shipmentId));
    }

}
