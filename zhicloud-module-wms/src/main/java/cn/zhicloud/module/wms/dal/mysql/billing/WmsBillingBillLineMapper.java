package cn.zhicloud.module.wms.dal.mysql.billing;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 3PL 计费账单明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsBillingBillLineMapper extends BaseMapperX<WmsBillingBillLineDO> {

    default List<WmsBillingBillLineDO> selectListByBillId(Long billId) {
        return selectList(WmsBillingBillLineDO::getBillId, billId);
    }

    default List<WmsBillingBillLineDO> selectListByBillIds(Collection<Long> billIds) {
        return selectList(new LambdaQueryWrapperX<WmsBillingBillLineDO>()
                .inIfPresent(WmsBillingBillLineDO::getBillId, billIds)
                .orderByAsc(WmsBillingBillLineDO::getBillId)
                .orderByAsc(WmsBillingBillLineDO::getId));
    }

    default void deleteByBillId(Long billId) {
        delete(WmsBillingBillLineDO::getBillId, billId);
    }

}
