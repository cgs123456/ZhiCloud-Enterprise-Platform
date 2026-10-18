package cn.zhicloud.module.wms.dal.dataobject.inventory;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.enums.order.WmsOrderTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 库存流水 DO
 *
 * @author 智云
 */
@TableName("wms_inventory_history")
@KeySequence("wms_inventory_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsInventoryHistoryDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;

    // ========= 库存维度相关字段 =========

    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 库位编号
     *
     * 关联 {@link WmsLocationDO#getId()}；0 = 未定位（V211 行级流水库位维度，ADR 269 D7）
     */
    private Long locationId;
    /**
     * 商品 SKU 编号
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;
    /**
     * 库存变化数量
     */
    private BigDecimal quantity;
    /**
     * 变化前库存数量
     */
    private BigDecimal beforeQuantity;
    /**
     * 变化后库存数量
     */
    private BigDecimal afterQuantity;

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

    // ========= 来源单据相关字段 =========

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

}
