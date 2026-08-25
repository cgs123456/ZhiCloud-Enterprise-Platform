package cn.zhicloud.module.erp.dal.dataobject.finance.tax;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 发票明细 DO
 *
 * @author 智云
 */
@TableName("erp_tax_invoice_line")
@KeySequence("erp_tax_invoice_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpTaxInvoiceLineDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long invoiceId;
    private Integer lineNo;
    private String productName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    private String remark;

}
