package cn.zhicloud.module.erp.dal.mysql.stock.vmi;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventoryPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiInventoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP VMI 供应商管理库存 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpVmiInventoryMapper extends BaseMapperX<ErpVmiInventoryDO> {

    default PageResult<ErpVmiInventoryDO> selectPage(ErpVmiInventoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpVmiInventoryDO>()
                .eqIfPresent(ErpVmiInventoryDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpVmiInventoryDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpVmiInventoryDO::getProductId, reqVO.getProductId())
                .likeIfPresent(ErpVmiInventoryDO::getProductName, reqVO.getProductName())
                .orderByDesc(ErpVmiInventoryDO::getId));
    }

    /**
     * 查询需要补货的库存（可用库存 <= 补货点）
     */
    default List<ErpVmiInventoryDO> selectReplenishmentList() {
        return selectList(new LambdaQueryWrapperX<ErpVmiInventoryDO>()
                .apply("available_quantity <= replenishment_point")
                .orderByAsc(ErpVmiInventoryDO::getSupplierId));
    }

    default ErpVmiInventoryDO selectBySupplierWarehouseProduct(Long supplierId, Long warehouseId, Long productId) {
        return selectOne(new LambdaQueryWrapperX<ErpVmiInventoryDO>()
                .eq(ErpVmiInventoryDO::getSupplierId, supplierId)
                .eq(ErpVmiInventoryDO::getWarehouseId, warehouseId)
                .eq(ErpVmiInventoryDO::getProductId, productId));
    }

}
