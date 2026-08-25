package cn.zhicloud.module.crm.dal.dataobject.salesorder;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.crm.dal.dataobject.product.CrmProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * CRM 销售订单明细 DO
 *
 * @author dhb52
 */
@TableName(value = "crm_sale_order_item")
@KeySequence("crm_sale_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmSaleOrderItemDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 订单编号
     *
     * 关联 {@link CrmSaleOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 产品编号
     *
     * 关联 {@link CrmProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单价，单位：元
     */
    private BigDecimal unitPrice;
    /**
     * 折扣
     */
    private BigDecimal discount;
    /**
     * 金额，单位：元
     */
    private BigDecimal amount;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 税额，单位：元
     */
    private BigDecimal taxAmount;
    /**
     * 备注
     */
    private String remark;

}
