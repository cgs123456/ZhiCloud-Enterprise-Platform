package cn.zhicloud.module.wms.service.inventory.transfer;

import java.math.BigDecimal;

/**
 * WMS 库存移库 Service 接口
 *
 * <p>V211（ADR 269 D3）：库内同仓跨位真转移——PDA confirm-move 的唯一写路径。
 *
 * @author 智云
 */
public interface WmsInventoryTransferService {

    /**
     * 库内同仓跨位移库（from ≠ to；跨仓拒绝走移库单）
     *
     * @param fromLocationId 来源库位编号
     * @param toLocationId   目标库位编号
     * @param skuId          SKU 编号
     * @param quantity       移库数量（正数）
     */
    void transfer(Long fromLocationId, Long toLocationId, Long skuId, BigDecimal quantity);

}
