package cn.zhicloud.module.wms.service.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * WMS 库存行级变更记录（包内值对象）
 *
 * <p>V211（ADR 269 D7）：行级流水素材——P-deduct 跨位分配由「明细聚合单条流水」改
 * 「参与行逐行一条」后，以本对象承载单行的库位与前后量；非分配明细为单行记录
 *（quantity = 明细原量，before/after = 行级前后量）。
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
class WmsInventoryRowChange {

    /**
     * 库位编号（0 = 未定位）
     */
    private final Long locationId;
    /**
     * 行变更前数量
     */
    private final BigDecimal beforeQuantity;
    /**
     * 行变更后数量
     */
    private final BigDecimal afterQuantity;
    /**
     * 行变更数量（分配行为该行 take；非分配明细为明细原量）
     */
    private final BigDecimal quantity;

}
