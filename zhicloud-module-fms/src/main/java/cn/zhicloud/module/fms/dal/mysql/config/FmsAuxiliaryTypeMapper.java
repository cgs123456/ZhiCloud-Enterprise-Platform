package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 辅助核算类别 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsAuxiliaryTypeMapper extends BaseMapperX<FmsAuxiliaryTypeDO> {

    default List<FmsAuxiliaryTypeDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAuxiliaryTypeDO>()
                .eq(FmsAuxiliaryTypeDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAuxiliaryTypeDO::getId));
    }

    default FmsAuxiliaryTypeDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsAuxiliaryTypeDO>()
                .eq(FmsAuxiliaryTypeDO::getAccountSetId, accountSetId)
                .eq(FmsAuxiliaryTypeDO::getId, id));
    }

    default FmsAuxiliaryTypeDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsAuxiliaryTypeDO>()
                .eq(FmsAuxiliaryTypeDO::getAccountSetId, accountSetId)
                .eq(FmsAuxiliaryTypeDO::getName, value));
    }

}
