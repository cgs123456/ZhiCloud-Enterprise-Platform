package cn.iocoder.yudao.module.wms.dal.dataobject.order.crossdock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 越库单明细 DO
 *
 * @author 芋道源码
 */
@TableName("wms_cross_dock_order_detail")
@KeySequence("wms_cross_dock_order_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsCrossDockOrderDetailDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 越库单编号
     *
     * 关联 {@link WmsCrossDockOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 商品 SKU 编号
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 行金额（数量 * 单价）
     */
    private BigDecimal amount;
    /**
     * 关联入库明细编号
     */
    private Long receiptDetailId;
    /**
     * 关联出库明细编号
     */
    private Long shipmentDetailId;
    /**
     * 备注
     */
    private String remark;

}
