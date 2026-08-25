package cn.zhicloud.module.wms.service.inventory.dto;

import cn.zhicloud.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.zhicloud.module.wms.enums.order.WmsOrderTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * WMS 库存变更请求 DTO
 *
 * @author 智云
 */
@Data
public class WmsInventoryChangeReqDTO {

    /**
     * 单据编号
     */
    private Long orderId;
    /**
     * 单据号
     */
    private String orderNo;
    /**
     * 单据类型
     *
     * 枚举 {@link WmsOrderTypeEnum#getType()}
     */
    private Integer orderType;

    /**
     * 库存变更明细
     */
    private List<Item> items;

    /**
     * WMS 库存变更明细
     */
    @Data
    public static class Item {

        /**
         * SKU 编号
         */
        private Long skuId;
        /**
         * 仓库编号
         */
        private Long warehouseId;
        /**
         * 变更数量（始终为正数，方向由 {@link #changeType} 决定）
         */
        private BigDecimal quantity;

        // ========= 单价备注相关字段 =========

        /**
         * 单价
         */
        private BigDecimal price;
        /**
         * 库存变化金额
         */
        private BigDecimal totalPrice;
        /**
         * 备注
         */
        private String remark;

        // ========= P0-7 库存三维字段贯通 =========

        /**
         * 库存变更类型
         *
         * 枚举 {@link WmsInventoryChangeTypeEnum}
         * - null / PLAIN：兼容旧逻辑，仅按 {@link #quantity} 正负增减 quantity 字段
         * - IN：入库，quantity + qty，available + qty
         * - OUT：出库，quantity - qty，available - qty（校验 available >= qty）
         * - LOCK：锁定，available - qty，locked + qty（校验 available >= qty）
         * - UNLOCK：解锁，available + qty，locked - qty（校验 locked >= qty）
         * - FREEZE：冻结，available - qty，frozen + qty（校验 available >= qty）
         * - UNFREEZE：解冻，available + qty，frozen - qty（校验 frozen >= qty）
         */
        private WmsInventoryChangeTypeEnum changeType;

    }

}
