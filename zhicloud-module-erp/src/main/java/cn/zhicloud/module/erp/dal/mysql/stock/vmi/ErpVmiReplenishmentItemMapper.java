package cn.zhicloud.module.erp.dal.mysql.stock.vmi;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP VMI 补货建议明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpVmiReplenishmentItemMapper extends BaseMapperX<ErpVmiReplenishmentItemDO> {

    default List<ErpVmiReplenishmentItemDO> selectListByReplenishmentId(Long replenishmentId) {
        return selectList(new LambdaQueryWrapperX<ErpVmiReplenishmentItemDO>()
                .eq(ErpVmiReplenishmentItemDO::getReplenishmentId, replenishmentId)
                .orderByAsc(ErpVmiReplenishmentItemDO::getId));
    }

    default int deleteByReplenishmentId(Long replenishmentId) {
        return delete(new LambdaQueryWrapperX<ErpVmiReplenishmentItemDO>()
                .eq(ErpVmiReplenishmentItemDO::getReplenishmentId, replenishmentId));
    }

}
