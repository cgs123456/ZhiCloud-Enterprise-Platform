package cn.iocoder.yudao.module.wms.dal.dataobject.inventory;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 库存 DO
 *
 * @author 芋道源码
 */
@TableName("wms_inventory")
@KeySequence("wms_inventory_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsInventoryDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 乐观锁版本号（P2：@Version 并发保护）
     */
    @Version
    private Long version;
    /**
     * 商品 SKU 编号
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;
    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 库存数量
     *
     * 物理库存总量
     */
    private BigDecimal quantity;
    /**
     * 可用数量
     *
     * 可被分配出库的数量 = quantity - lockedQuantity - frozenQuantity
     */
    private BigDecimal availableQuantity;
    /**
     * 锁定数量
     *
     * 已被订单/波次预占但未出库的数量
     */
    private BigDecimal lockedQuantity;
    /**
     * 冻结数量
     *
     * 因质检、盘点等冻结的数量
     */
    private BigDecimal frozenQuantity;
    /**
     * 货主编号
     *
     * 关联 {@link cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO#getId()}
     * 3PL 场景下用于区分不同货主的库存归属
     */
    private Long ownerId;
    /**
     * 备注
     */
    private String remark;

}
