package cn.zhicloud.module.wms.dal.mysql.md.item;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.md.item.vo.item.WmsItemListReqVO;
import cn.zhicloud.module.wms.controller.admin.md.item.vo.item.WmsItemPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * WMS 商品 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsItemMapper extends BaseMapperX<WmsItemDO> {

    default PageResult<WmsItemDO> selectPage(WmsItemPageReqVO reqVO, Collection<Long> categoryIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsItemDO>()
                .likeIfPresent(WmsItemDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsItemDO::getName, reqVO.getName())
                .inIfPresent(WmsItemDO::getCategoryId, categoryIds)
                .eqIfPresent(WmsItemDO::getBrandId, reqVO.getBrandId())
                .betweenIfPresent(WmsItemDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsItemDO::getId));
    }

    default List<WmsItemDO> selectList(WmsItemListReqVO reqVO, Collection<Long> categoryIds) {
        return selectList(new LambdaQueryWrapperX<WmsItemDO>()
                .likeIfPresent(WmsItemDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsItemDO::getName, reqVO.getName())
                .inIfPresent(WmsItemDO::getCategoryId, categoryIds)
                .eqIfPresent(WmsItemDO::getBrandId, reqVO.getBrandId())
                .betweenIfPresent(WmsItemDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsItemDO::getId));
    }

    default WmsItemDO selectByName(String name) {
        return selectOne(WmsItemDO::getName, name);
    }

    default WmsItemDO selectByCode(String code) {
        return selectOne(WmsItemDO::getCode, code);
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(WmsItemDO::getCategoryId, categoryId);
    }

    default Long selectCountByBrandId(Long brandId) {
        return selectCount(WmsItemDO::getBrandId, brandId);
    }

    /**
     * 将未出现在本轮出库流水中的商品 ABC 分类重置为 C（下推 SQL，避免全表加载）
     *
     * @param excludeIds 已分类的商品编号集合；为空时全表更新为 C
     * @return 影响行数
     */
    @Update("<script>"
            + "UPDATE wms_item SET abc_classification = 'C', update_time = NOW() WHERE deleted = 0"
            + "<if test='excludeIds != null and excludeIds.size() > 0'>"
            + " AND id NOT IN "
            + "<foreach collection='excludeIds' item='excludeId' open='(' separator=',' close=')'>#{excludeId}</foreach>"
            + "</if>"
            + "</script>")
    int updateAbcClassificationToCExcluding(@Param("excludeIds") Collection<Long> excludeIds);

}
