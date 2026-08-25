package cn.zhicloud.module.wms.dal.mysql.order.asn;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS ASN 到货通知单明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsAsnOrderDetailMapper extends BaseMapperX<WmsAsnOrderDetailDO> {

    default List<WmsAsnOrderDetailDO> selectListByAsnOrderId(Long asnOrderId) {
        return selectList(WmsAsnOrderDetailDO::getAsnOrderId, asnOrderId);
    }

    default List<WmsAsnOrderDetailDO> selectListByAsnOrderIds(Collection<Long> asnOrderIds) {
        return selectList(new LambdaQueryWrapperX<WmsAsnOrderDetailDO>()
                .inIfPresent(WmsAsnOrderDetailDO::getAsnOrderId, asnOrderIds)
                .orderByAsc(WmsAsnOrderDetailDO::getId));
    }

    default void deleteByAsnOrderId(Long asnOrderId) {
        delete(WmsAsnOrderDetailDO::getAsnOrderId, asnOrderId);
    }

}
