package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceIndicatorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 财务指标 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsFinanceIndicatorMapper extends BaseMapperX<FmsFinanceIndicatorDO> {

    default List<FmsFinanceIndicatorDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsFinanceIndicatorDO>()
                .eq(FmsFinanceIndicatorDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsFinanceIndicatorDO::getId));
    }

    default FmsFinanceIndicatorDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsFinanceIndicatorDO>()
                .eq(FmsFinanceIndicatorDO::getAccountSetId, accountSetId)
                .eq(FmsFinanceIndicatorDO::getId, id));
    }

    default FmsFinanceIndicatorDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsFinanceIndicatorDO>()
                .eq(FmsFinanceIndicatorDO::getAccountSetId, accountSetId)
                .eq(FmsFinanceIndicatorDO::getCode, value));
    }

}
