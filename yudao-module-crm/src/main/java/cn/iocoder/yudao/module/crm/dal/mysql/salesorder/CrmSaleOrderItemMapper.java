package cn.iocoder.yudao.module.crm.dal.mysql.salesorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CRM 销售订单明细 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmSaleOrderItemMapper extends BaseMapperX<CrmSaleOrderItemDO> {

    default List<CrmSaleOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<CrmSaleOrderItemDO>()
                .eq(CrmSaleOrderItemDO::getOrderId, orderId)
                .orderByAsc(CrmSaleOrderItemDO::getId));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<CrmSaleOrderItemDO>()
                .eq(CrmSaleOrderItemDO::getOrderId, orderId));
    }

}
