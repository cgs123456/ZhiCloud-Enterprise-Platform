package cn.iocoder.yudao.module.wms.dal.mysql.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 安全库存配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsSafetyStockConfigMapper extends BaseMapperX<WmsSafetyStockConfigDO> {

    default PageResult<WmsSafetyStockConfigDO> selectPage(WmsSafetyStockConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsSafetyStockConfigDO>()
                .eqIfPresent(WmsSafetyStockConfigDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsSafetyStockConfigDO::getProductId, reqVO.getProductId())
                .orderByDesc(WmsSafetyStockConfigDO::getId));
    }

    default WmsSafetyStockConfigDO selectByWarehouseIdAndProductId(Long warehouseId, Long productId) {
        return selectOne(new LambdaQueryWrapperX<WmsSafetyStockConfigDO>()
                .eq(WmsSafetyStockConfigDO::getWarehouseId, warehouseId)
                .eq(WmsSafetyStockConfigDO::getProductId, productId));
    }

    default List<WmsSafetyStockConfigDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(WmsSafetyStockConfigDO::getWarehouseId, warehouseId);
    }

    default List<WmsSafetyStockConfigDO> selectListByProductIds(Collection<Long> productIds) {
        return selectList(new LambdaQueryWrapperX<WmsSafetyStockConfigDO>()
                .in(WmsSafetyStockConfigDO::getProductId, productIds));
    }

}