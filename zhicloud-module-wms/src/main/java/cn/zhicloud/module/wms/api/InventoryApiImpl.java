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
        // V205 库位维度接入（ADR 259 D8）：同一 (sku, warehouse) 现可对应多行（分位行），
        // 可用量语义 = 全部行 available 合计；未定位单行场景与基线口径等价
        BigDecimal available = BigDecimal.ZERO;
        for (WmsInventoryDO inventory : inventoryMapper.selectListBySkuIdAndWarehouseId(skuId, warehouseId)) {
            if (inventory.getAvailableQuantity() != null) {
                available = available.add(inventory.getAvailableQuantity());
            }
        }
        return available;
    }

    @Override
    public boolean isSufficient(Long skuId, Long warehouseId, BigDecimal requiredQty) {
        if (requiredQty == null || requiredQty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return getAvailableQuantity(skuId, warehouseId).compareTo(requiredQty) >= 0;
    }

}
