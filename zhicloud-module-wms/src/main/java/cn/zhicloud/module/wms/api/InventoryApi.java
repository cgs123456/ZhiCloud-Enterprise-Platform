package cn.zhicloud.module.wms.api;

import java.math.BigDecimal;

/**
 * WMS 库存 API
 *
 * <p>对外暴露库存查询能力，作为「单一库存真值源」（P1-1）的统一查询入口。
 * ERP/MES 等模块查询库存时应经本 API，而非直接 import WMS 的 dal/service（P1-3 模块边界治理）。
 *
 * @author 智云
 */
public interface InventoryApi {

    /**
     * 查询指定维度库存的可用量
     *
     * @param skuId       商品 SKU ID
     * @param warehouseId 仓库 ID
     * @return 可用量（无库存行时返回 0）
     */
    BigDecimal getAvailableQuantity(Long skuId, Long warehouseId);

    /**
     * 判断指定维度库存是否满足需求量
     *
     * @param skuId       商品 SKU ID
     * @param warehouseId 仓库 ID
     * @param requiredQty 需求量（正数）
     * @return true=可用量 >= 需求量
     */
    boolean isSufficient(Long skuId, Long warehouseId, BigDecimal requiredQty);

}
