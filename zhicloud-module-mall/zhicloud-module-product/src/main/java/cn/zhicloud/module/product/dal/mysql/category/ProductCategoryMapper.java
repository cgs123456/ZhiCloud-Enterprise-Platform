package cn.zhicloud.module.product.dal.mysql.category;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.product.controller.admin.category.vo.ProductCategoryListReqVO;
import cn.zhicloud.module.product.dal.dataobject.category.ProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 商品分类 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapperX<ProductCategoryDO> {

    default List<ProductCategoryDO> selectList(ProductCategoryListReqVO listReqVO) {
        return selectList(new LambdaQueryWrapperX<ProductCategoryDO>()
                .likeIfPresent(ProductCategoryDO::getName, listReqVO.getName())
                .eqIfPresent(ProductCategoryDO::getParentId, listReqVO.getParentId())
                .inIfPresent(ProductCategoryDO::getId, listReqVO.getParentIds())
                .eqIfPresent(ProductCategoryDO::getStatus, listReqVO.getStatus())
                .orderByDesc(ProductCategoryDO::getId));
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(ProductCategoryDO::getParentId, parentId);
    }

    default List<ProductCategoryDO> selectListByStatus(Integer status) {
        return selectList(ProductCategoryDO::getStatus, status);
    }

    default List<ProductCategoryDO> selectListByIdAndStatus(Collection<Long> ids, Integer status) {
        return selectList(new LambdaQueryWrapperX<ProductCategoryDO>()
                .in(ProductCategoryDO::getId, ids)
                .eq(ProductCategoryDO::getStatus, status));
    }

}
