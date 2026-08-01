package cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpInvoiceStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpInvoiceTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 发票主表 DO
 *
 * @author 芋道源码
 */
@TableName("erp_tax_invoice")
@KeySequence("erp_tax_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpTaxInvoiceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String invoiceNo;
    private String invoiceCode;
    private Integer invoiceType;
    private String buyerName;
    private String buyerTaxNo;
    private String sellerName;
    private String sellerTaxNo;
    private LocalDate invoiceDate;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    private Integer status;
    private String sourceOrderType;
    private Long sourceOrderId;
    private String remark;

}
