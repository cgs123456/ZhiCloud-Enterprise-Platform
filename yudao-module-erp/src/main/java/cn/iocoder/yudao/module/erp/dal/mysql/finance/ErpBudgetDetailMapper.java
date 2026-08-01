package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBudgetDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 预算明细 Mapper（P0-14）
 *
 * @author 芋道源码
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
