package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceParameterDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * FMS 账套参数 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsFinanceParameterMapper extends BaseMapperX<FmsFinanceParameterDO> {

    default FmsFinanceParameterDO selectByAccountSet(Long accountSetId) {
        return selectOne(FmsFinanceParameterDO::getAccountSetId, accountSetId);
    }

}
