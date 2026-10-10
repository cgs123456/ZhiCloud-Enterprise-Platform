package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsDigestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 常用摘要 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsDigestMapper extends BaseMapperX<FmsDigestDO> {

    default List<FmsDigestDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsDigestDO>()
                .eq(FmsDigestDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsDigestDO::getId));
    }

    default FmsDigestDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsDigestDO>()
                .eq(FmsDigestDO::getAccountSetId, accountSetId)
                .eq(FmsDigestDO::getId, id));
    }

}
