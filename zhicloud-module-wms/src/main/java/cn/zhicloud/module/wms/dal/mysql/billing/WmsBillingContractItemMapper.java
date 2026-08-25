package cn.zhicloud.module.wms.dal.mysql.billing;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingContractItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 3PL 计费合同条款 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsBillingContractItemMapper extends BaseMapperX<WmsBillingContractItemDO> {

    default List<WmsBillingContractItemDO> selectListByContractId(Long contractId) {
        return selectList(WmsBillingContractItemDO::getContractId, contractId);
    }

    default List<WmsBillingContractItemDO> selectListByContractIds(Collection<Long> contractIds) {
        return selectList(new LambdaQueryWrapperX<WmsBillingContractItemDO>()
                .inIfPresent(WmsBillingContractItemDO::getContractId, contractIds)
                .orderByAsc(WmsBillingContractItemDO::getContractId)
                .orderByAsc(WmsBillingContractItemDO::getId));
    }

    default void deleteByContractId(Long contractId) {
        delete(WmsBillingContractItemDO::getContractId, contractId);
    }

}
