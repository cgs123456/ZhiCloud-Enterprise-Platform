package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 凭证字 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsVoucherWordMapper extends BaseMapperX<FmsVoucherWordDO> {

    default List<FmsVoucherWordDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsVoucherWordDO>()
                .eq(FmsVoucherWordDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsVoucherWordDO::getId));
    }

    default FmsVoucherWordDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherWordDO>()
                .eq(FmsVoucherWordDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherWordDO::getId, id));
    }

    default FmsVoucherWordDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherWordDO>()
                .eq(FmsVoucherWordDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherWordDO::getName, value));
    }

}
