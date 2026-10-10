package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemPageReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 辅助核算项目 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsAuxiliaryItemMapper extends BaseMapperX<FmsAuxiliaryItemDO> {

    default List<FmsAuxiliaryItemDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAuxiliaryItemDO>()
                .eq(FmsAuxiliaryItemDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAuxiliaryItemDO::getId));
    }

    default FmsAuxiliaryItemDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsAuxiliaryItemDO>()
                .eq(FmsAuxiliaryItemDO::getAccountSetId, accountSetId)
                .eq(FmsAuxiliaryItemDO::getId, id));
    }

    default FmsAuxiliaryItemDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsAuxiliaryItemDO>()
                .eq(FmsAuxiliaryItemDO::getAccountSetId, accountSetId)
                .eq(FmsAuxiliaryItemDO::getCode, value));
    }

    default PageResult<FmsAuxiliaryItemDO> selectPage(FmsAuxiliaryItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FmsAuxiliaryItemDO>()
                .eqIfPresent(FmsAuxiliaryItemDO::getAccountSetId, reqVO.getAccountSetId())
                .eqIfPresent(FmsAuxiliaryItemDO::getAuxiliaryTypeId, reqVO.getAuxiliaryTypeId())
                .and(reqVO.getSearch() != null, w -> w.like(FmsAuxiliaryItemDO::getCode, reqVO.getSearch()).or().like(FmsAuxiliaryItemDO::getName, reqVO.getSearch()))
                .orderByAsc(FmsAuxiliaryItemDO::getId));
    }

}
