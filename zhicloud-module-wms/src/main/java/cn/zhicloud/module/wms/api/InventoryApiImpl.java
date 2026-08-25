package cn.zhicloud.module.wms.api;

import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * WMS 库存 API 实现
 *
 * @author 智云
 */
@Service
@Validated
public class InventoryApiImpl implements InventoryApi {

    @Resource
    private WmsInventoryMapper inventoryMapper;

    @Override
    public BigDecimal getAvailableQuantity(Long skuId, Long warehouseId) {
        WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(skuId, warehouseId);
        return inventory == null || inventory.getAvailableQuantity() == null
                ? BigDecimal.ZERO : inventory.getAvailableQuantity();
    }

    @Override
    public boolean isSufficient(Long skuId, Long warehouseId, BigDecimal requiredQty) {
        if (requiredQty == null || requiredQty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return getAvailableQuantity(skuId, warehouseId).compareTo(requiredQty) >= 0;
    }

}
