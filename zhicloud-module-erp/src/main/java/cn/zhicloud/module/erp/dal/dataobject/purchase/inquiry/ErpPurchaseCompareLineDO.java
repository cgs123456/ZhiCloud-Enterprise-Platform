package cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 采购比价单明细行 DO
 *
 * @author 智云
 */
@TableName("erp_purchase_compare_line")
@KeySequence("erp_purchase_compare_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseCompareLineDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 比价单编号
     *
     * 关联 {@link ErpPurchaseCompareDO#getId()}
     */
    private Long compareId;
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
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 报价单明细编号
     *
     * 关联 {@link ErpPurchaseQuoteItemDO#getId()}
     */
    private Long quoteItemId;
    /**
     * 报价单价，单位：元
     */
    private BigDecimal unitPrice;
    /**
     * 报价金额，单位：元
     */
    private BigDecimal amount;
    /**
     * 报价交货日期
     */
    private LocalDate deliveryDate;
    /**
     * 是否推荐
     *
     * 枚举：true 是 / false 否
     */
    private Boolean isRecommended;

}
