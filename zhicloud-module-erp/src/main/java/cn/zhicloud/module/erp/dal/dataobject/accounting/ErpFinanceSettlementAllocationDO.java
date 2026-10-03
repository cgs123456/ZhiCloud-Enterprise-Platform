package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 财务结算分配 DO。
 *
 * <p>记录收付款明细对发票及业务来源的核销分配，支持预付、预收和冲销。
 *
 * @author 智云
 */
@TableName("erp_finance_settlement_allocation")
@KeySequence("erp_finance_settlement_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFinanceSettlementAllocationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String financeDocType;
    private Long financeDocId;
    private Long financeDocItemId;
    private Long invoiceId;
    private Long invoiceLineId;
    private String sourceDocType;
    private Long sourceDocId;
    private BigDecimal allocatedAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Integer status;
    private Long reversalOfAllocationId;
    private LocalDateTime allocatedAt;
    private String lastError;

}
