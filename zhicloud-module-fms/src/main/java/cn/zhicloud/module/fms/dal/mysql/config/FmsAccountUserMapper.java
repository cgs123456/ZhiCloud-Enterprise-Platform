package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 账套成员 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsAccountUserMapper extends BaseMapperX<FmsAccountUserDO> {

    default List<FmsAccountUserDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAccountUserDO>()
                .eq(FmsAccountUserDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAccountUserDO::getId));
    }

    default FmsAccountUserDO selectByAccountSetAndUser(Long accountSetId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<FmsAccountUserDO>()
                .eq(FmsAccountUserDO::getAccountSetId, accountSetId)
                .eq(FmsAccountUserDO::getUserId, userId));
    }

    default List<FmsAccountUserDO> selectListByUser(Long userId) {
        return selectList(new LambdaQueryWrapperX<FmsAccountUserDO>()
                .eq(FmsAccountUserDO::getUserId, userId));
    }

}
