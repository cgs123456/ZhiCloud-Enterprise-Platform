package cn.zhicloud.module.erp.service.accounting.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 会计事件创建请求。
 *
 * @author 智云
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountingEventCreateReqBO {

    private String eventType;
    private String sourceDocType;
    private Long sourceDocId;
    private String sourceDocNo;
    private Long sourceDocLineId;
    private Integer sourceRevision;
    private LocalDate eventDate;
    private LocalDate accountingDate;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    /**
     * 成本金额。销售出库成本结转使用，其他事件为 0。
     */
    private BigDecimal costAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long reversalOfEventId;
    private LocalDateTime occurredAt;

}
