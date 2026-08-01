package cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 采购报价单明细 DO
 *
 * @author 芋道源码
 */
@TableName("erp_purchase_quote_item")
@KeySequence("erp_purchase_quote_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseQuoteItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 报价单编号
     *
     * 关联 {@link ErpPurchaseQuoteDO#getId()}
     */
    private Long quoteId;
    /**
     * 询价单明细编号
     *
     * 关联 {@link ErpPurchaseInquiryItemDO#getId()}
     */
    private Long inquiryItemId;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 报价单价，单位：元
     */
    private BigDecimal unitPrice;
    /**
     * 报价金额，单位：元
     *
     * amount = unitPrice * quantity
     */
    private BigDecimal amount;
    /**
     * 报价交货日期
     */
    private LocalDate deliveryDate;
    /**
     * 备注
     */
    private String remark;

}
