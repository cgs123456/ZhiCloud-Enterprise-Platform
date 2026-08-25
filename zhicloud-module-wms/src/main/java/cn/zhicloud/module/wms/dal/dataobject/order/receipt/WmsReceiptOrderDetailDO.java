package cn.zhicloud.module.wms.dal.dataobject.order.receipt;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WMS 入库单明细 DO
 *
 * @author 智云
 */
@TableName("wms_receipt_order_detail")
@KeySequence("wms_receipt_order_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsReceiptOrderDetailDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    // ========= 单据商品字段 =========

    /**
     * 入库单编号
     *
     * 关联 {@link WmsReceiptOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 商品 SKU 编号
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;

    // ========= 仓库字段 =========

    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;

    // ========= 数量金额字段 =========

    /**
     * 入库数量
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal price;
    /**
     * 行金额（数量 * 单价）
     */
    private BigDecimal totalPrice;

    // ========= 批次/效期字段（P0-13 新增） =========

    /**
     * 批次号（FEFO 出库 + 上架 Slotting 用）
     */
    private String batchNo;
    /**
     * 生产日期
     */
    private LocalDate productionDate;
    /**
     * 过期日期（空表示无保质期管理）
     */
    private LocalDate expiryDate;

}
