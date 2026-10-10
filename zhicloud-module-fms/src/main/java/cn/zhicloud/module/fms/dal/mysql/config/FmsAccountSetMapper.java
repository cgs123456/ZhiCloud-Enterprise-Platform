package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 账套 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsAccountSetMapper extends BaseMapperX<FmsAccountSetDO> {

    default FmsAccountSetDO selectByCompanyCode(String companyCode) {
        return selectOne(new LambdaQueryWrapperX<FmsAccountSetDO>()
                .eq(FmsAccountSetDO::getCompanyCode, companyCode));
    }

    default List<FmsAccountSetDO> selectListByIds(List<Long> ids) {
        return selectList(new LambdaQueryWrapperX<FmsAccountSetDO>()
                .in(FmsAccountSetDO::getId, ids)
                .orderByAsc(FmsAccountSetDO::getId));
    }

}
