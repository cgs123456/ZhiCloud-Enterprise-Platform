package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 期初余额 Mapper（账套口径，C6③ / ADR 265）
 *
 * <p>账套维度查询 (tenant_id, account_set_id) 由唯一键前缀承载（V209），
 * 不另建冗余索引。
 *
 * @author 智云
 */
@Mapper
public interface ErpGlInitialBalanceMapper extends BaseMapperX<ErpGlInitialBalanceDO> {

    /**
     * 查询账套下的期初余额行
     *
     * @param accountSetId 账套编号
     * @return 期初余额行列表（无行时为空列表）
     */
    default List<ErpGlInitialBalanceDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<ErpGlInitialBalanceDO>()
                .eq(ErpGlInitialBalanceDO::getAccountSetId, accountSetId));
    }

}
