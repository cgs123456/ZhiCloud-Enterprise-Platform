package cn.iocoder.yudao.module.wms.dal.dataobject.order.wave;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 波次单明细 DO
 *
 * <p>明细行由波次生成时根据所选出库单展开而来，每个 SKU 一行。
 *
 * @author 芋道源码
 */
@TableName("wms_wave_order_detail")
@KeySequence("wms_wave_order_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsWaveOrderDetailDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 波次单 ID
     *
     * 关联 {@link WmsWaveOrderDO#getId()}
     */
    private Long waveOrderId;
    /**
     * 出库单 ID
     *
     * 关联 {@link WmsShipmentOrderDO#getId()}
     */
    private Long shipmentOrderId;
    /**
     * 商品规格 ID
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;
    /**
     * 拣货数量
     */
    private BigDecimal pickQuantity;
    /**
     * 已拣数量
     */
    private BigDecimal pickedQuantity;
    /**
     * 备注
     */
    private String remark;

}
