package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsCurrencyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 币别 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsCurrencyMapper extends BaseMapperX<FmsCurrencyDO> {

    default List<FmsCurrencyDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsCurrencyDO>()
                .eq(FmsCurrencyDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsCurrencyDO::getId));
    }

    default FmsCurrencyDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsCurrencyDO>()
                .eq(FmsCurrencyDO::getAccountSetId, accountSetId)
                .eq(FmsCurrencyDO::getId, id));
    }

    default FmsCurrencyDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsCurrencyDO>()
                .eq(FmsCurrencyDO::getAccountSetId, accountSetId)
                .eq(FmsCurrencyDO::getCode, value));
    }

}
