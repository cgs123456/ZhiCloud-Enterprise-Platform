package cn.iocoder.yudao.module.crm.dal.dataobject.invoice;

import cn.iocoder.yudao.framework.mybatis.core.type.StringListTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.iocoder.yudao.module.crm.enums.common.CrmAuditStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CRM 开票 DO
 *
 * @author 芋道源码
 */
@TableName(value = "crm_invoice", autoResultMap = true)
@KeySequence("crm_invoice_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmInvoiceDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 开票单号
     */
    private String no;
    /**
     * 合同编号
     *
     * 关联 {@link CrmContractDO#getId()}
     */
    private Long contractId;
    /**
     * 客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 联系人编号
     *
     * 关联 {@link CrmContactDO#getId()}
     */
    private Long contactId;
    /**
     * 发票类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.invoice.CrmInvoiceTypeEnum}
     */
    private Integer invoiceType;
    /**
     * 发票号码
     */
    private String invoiceNo;
    /**
     * 购方名称
     */
    private String buyerName;
    /**
     * 购方税号
     */
    private String buyerTaxNo;
    /**
     * 不含税金额
     */
    private BigDecimal amountWithoutTax;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 含税金额
     */
    private BigDecimal amountWithTax;
    /**
     * 开票日期
     */
    private LocalDate invoiceDate;
    /**
     * 审批状态
     *
     * 枚举 {@link CrmAuditStatusEnum}
     */
    private Integer auditStatus;
    /**
     * 工作流编号
     *
     * 关联 ProcessInstance 的 id 属性
     */
    private String processInstanceId;
    /**
     * 负责人的用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long ownerUserId;
    /**
     * 发票附件 URL 列表
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> fileUrls;
    /**
     * 备注
     */
    private String remark;

}
