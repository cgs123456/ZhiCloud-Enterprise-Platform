package cn.zhicloud.module.wms.dal.dataobject.order.asn;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WMS ASN 到货通知单明细 DO
 *
 * @author 智云
 */
@TableName("wms_asn_order_detail")
@KeySequence("wms_asn_order_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsAsnOrderDetailDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * ASN 单编号
     *
     * 关联 {@link WmsAsnOrderDO#getId()}
     */
    private Long asnOrderId;
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
     * 预计数量
     */
    private BigDecimal expectedQuantity;
    /**
     * 已收数量
     */
    private BigDecimal receivedQuantity;
    /**
     * 单位
     */
    private String unit;
    /**
     * 批次号
     */
    private String lotNumber;
    /**
     * 生产日期
     */
    private LocalDate productionDate;
    /**
     * 过期日期
     */
    private LocalDate expiryDate;
    /**
     * 备注
     */
    private String remark;

}
