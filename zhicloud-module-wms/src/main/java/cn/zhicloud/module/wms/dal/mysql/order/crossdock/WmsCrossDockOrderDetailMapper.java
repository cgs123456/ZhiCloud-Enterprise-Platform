package cn.zhicloud.module.wms.dal.mysql.order.crossdock;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 越库单明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsCrossDockOrderDetailMapper extends BaseMapperX<WmsCrossDockOrderDetailDO> {

    default List<WmsCrossDockOrderDetailDO> selectListByOrderId(Long orderId) {
        return selectList(WmsCrossDockOrderDetailDO::getOrderId, orderId);
    }

    default List<WmsCrossDockOrderDetailDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(new LambdaQueryWrapperX<WmsCrossDockOrderDetailDO>()
                .inIfPresent(WmsCrossDockOrderDetailDO::getOrderId, orderIds)
                .orderByAsc(WmsCrossDockOrderDetailDO::getOrderId)
                .orderByAsc(WmsCrossDockOrderDetailDO::getId));
    }

    default void deleteByOrderId(Long orderId) {
        delete(WmsCrossDockOrderDetailDO::getOrderId, orderId);
    }

}
