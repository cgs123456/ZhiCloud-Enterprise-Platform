package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 发票来源关联 DO。
 *
 * <p>记录发票行与采购入库、销售出库、退货等业务行的来源关系，供三单匹配和
 * 税务勾稽使用。
 *
 * @author 智云
 */
@TableName("erp_tax_invoice_source")
@KeySequence("erp_tax_invoice_source_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpTaxInvoiceSourceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long invoiceId;
    private Long invoiceLineId;
    private String sourceDocType;
    private Long sourceDocId;
    private Long sourceDocLineId;
    private BigDecimal quantity;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    private Integer status;
    private String lastError;

}
