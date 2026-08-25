package cn.zhicloud.module.crm.dal.dataobject.invoice;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * CRM 开票明细 DO
 *
 * @author 智云
 */
@TableName("crm_invoice_line")
@KeySequence("crm_invoice_line_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmInvoiceLineDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 发票编号
     *
     * 关联 {@link CrmInvoiceDO#getId()}
     */
    private Long invoiceId;
    /**
     * 产品名称
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
     * 不含税金额
     */
    private BigDecimal amountWithoutTax;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 含税金额
     */
    private BigDecimal amountWithTax;
    /**
     * 备注
     */
    private String remark;

}
