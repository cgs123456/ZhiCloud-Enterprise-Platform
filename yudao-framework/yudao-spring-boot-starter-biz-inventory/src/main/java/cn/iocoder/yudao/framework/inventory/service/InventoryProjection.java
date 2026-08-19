package cn.iocoder.yudao.framework.inventory.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存投影快照（M2 日终对账用）
 *
 * <p>各业务模块（erp_stock / mes_wm_material_stock / wms_inventory）在 P1-1 阶段 A 退化为只读投影后，
 * 通过 {@link InventoryProjectionReader} 暴露自身当前库存快照，供共享 Starter 与真值源 {@code inventory_item} 比对。
 *
 * @author 智云库存治理
 */
@Data
public class InventoryProjection {

    /**
     * 来源标识：erp / mes / wms
     */
    private String source;

    private Long itemId;
    private Long warehouseId;
    private Long locationId;
    private Long areaId;
    private Long batchId;

    /**
     * 投影侧的库存数量
     */
    private BigDecimal quantity;

    /**
     * 投影侧的锁定数量
     */
    private BigDecimal lockedCount;

}
