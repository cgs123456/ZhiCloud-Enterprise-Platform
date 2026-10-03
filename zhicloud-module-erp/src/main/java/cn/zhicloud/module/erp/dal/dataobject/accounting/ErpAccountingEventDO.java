package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 会计事件 DO。
 *
 * <p>业务单据与总账之间的追加式事实记录。会计事件不得通过软删除改写，
 * 冲销必须新增 reversal_of_event_id 指向原事件的独立事件。
 *
 * @author 智云
 */
@TableName("erp_accounting_event")
@KeySequence("erp_accounting_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountingEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String eventId;
    private String eventType;
    private String sourceDocType;
    private Long sourceDocId;
    private String sourceDocNo;
    private Long sourceDocLineId;
    private Integer sourceRevision;
    /**
     * 期次序号。部分开票/收款/付款从 1 递增；主事件与冲销沿用原事件期次，非分期事件为 0。
     */
    private Integer sequenceNo;
    private LocalDate eventDate;
    private LocalDate accountingDate;
    private Long periodId;
    private String periodCode;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    /**
     * 成本金额。用于销售成本结转等需要独立于收入金额记账的场景，不涉及成本的事件为 0。
     */
    private BigDecimal costAmount;
    /**
     * 来源单据勾稽总额。同一单据同事件类型的带符号累计金额绝对值不得大于该值。
     * NULL 表示未参与勾稽守卫（历史数据默认值）。
     */
    private BigDecimal docTotalAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Integer status;
    private String idempotencyKey;
    private String ruleSetVersion;
    private String payloadHash;
    private Long reversalOfEventId;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime occurredAt;
    private LocalDateTime postedAt;

}
