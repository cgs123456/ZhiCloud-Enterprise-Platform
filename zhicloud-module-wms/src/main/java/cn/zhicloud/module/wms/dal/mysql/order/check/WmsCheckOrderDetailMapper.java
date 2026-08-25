package cn.zhicloud.module.wms.dal.mysql.order.check;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.dal.dataobject.order.check.WmsCheckOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 盘库单明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsCheckOrderDetailMapper extends BaseMapperX<WmsCheckOrderDetailDO> {

    default List<WmsCheckOrderDetailDO> selectListByOrderId(Long orderId) {
        return selectList(WmsCheckOrderDetailDO::getOrderId, orderId);
    }

    default List<WmsCheckOrderDetailDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(new LambdaQueryWrapperX<WmsCheckOrderDetailDO>()
                .inIfPresent(WmsCheckOrderDetailDO::getOrderId, orderIds)
                .orderByAsc(WmsCheckOrderDetailDO::getOrderId)
                .orderByAsc(WmsCheckOrderDetailDO::getId));
    }

    default void deleteByOrderId(Long orderId) {
        delete(WmsCheckOrderDetailDO::getOrderId, orderId);
    }

    default Long selectCountBySkuId(Long skuId) {
        return selectCount(WmsCheckOrderDetailDO::getSkuId, skuId);
    }

}
