package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 预算明细 Mapper（P0-14）
 *
 * @author 智云
 */
@Mapper
public interface ErpBudgetDetailMapper extends BaseMapperX<ErpBudgetDetailDO> {

    default List<ErpBudgetDetailDO> selectListByBudgetId(Long budgetId) {
        return selectList(ErpBudgetDetailDO::getBudgetId, budgetId);
    }

    default ErpBudgetDetailDO selectByBudgetIdAndAccountId(Long budgetId, Long accountId) {
        return selectOne(new LambdaQueryWrapperX<ErpBudgetDetailDO>()
                .eq(ErpBudgetDetailDO::getBudgetId, budgetId)
                .eq(ErpBudgetDetailDO::getAccountId, accountId));
    }

    default void deleteByBudgetId(Long budgetId) {
        delete(new LambdaQueryWrapperX<ErpBudgetDetailDO>()
                .eq(ErpBudgetDetailDO::getBudgetId, budgetId));
    }

}
