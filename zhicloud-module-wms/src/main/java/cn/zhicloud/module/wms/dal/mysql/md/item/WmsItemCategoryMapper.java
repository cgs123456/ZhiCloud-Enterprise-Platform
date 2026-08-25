package cn.zhicloud.module.wms.dal.mysql.md.item;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.md.item.vo.category.WmsItemCategoryListReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 商品分类 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsItemCategoryMapper extends BaseMapperX<WmsItemCategoryDO> {

    default List<WmsItemCategoryDO> selectList(WmsItemCategoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<WmsItemCategoryDO>()
                .eqIfPresent(WmsItemCategoryDO::getParentId, reqVO.getParentId())
                .likeIfPresent(WmsItemCategoryDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsItemCategoryDO::getName, reqVO.getName())
                .eqIfPresent(WmsItemCategoryDO::getStatus, reqVO.getStatus())
                .orderByAsc(WmsItemCategoryDO::getSort)
                .orderByAsc(WmsItemCategoryDO::getId));
    }

    default WmsItemCategoryDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(WmsItemCategoryDO::getParentId, parentId, WmsItemCategoryDO::getName, name);
    }

    default WmsItemCategoryDO selectByCode(String code) {
        return selectOne(WmsItemCategoryDO::getCode, code);
    }

    default List<WmsItemCategoryDO> selectListByParentIds(Collection<Long> parentIds) {
        return selectList(new LambdaQueryWrapperX<WmsItemCategoryDO>()
                .in(WmsItemCategoryDO::getParentId, parentIds)
                .orderByAsc(WmsItemCategoryDO::getSort)
                .orderByAsc(WmsItemCategoryDO::getId));
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(WmsItemCategoryDO::getParentId, parentId);
    }

}
