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
    /**
     * 期次序号。部分开票/收款/付款从 1 递增；非分期事件为 0。
     */
    private Integer sequenceNo;
    private LocalDate eventDate;
    private LocalDate accountingDate;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    /**
     * 成本金额。销售出库成本结转使用，其他事件为 0。
     */
    private BigDecimal costAmount;
    /**
     * 来源单据勾稽总额。由调用方（单据服务）传入，作为金额上界判定基线；
     * 为空时回退到同单据已落地事件的基线，仍取不到则拒绝过账。
     */
    private BigDecimal docTotalAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long reversalOfEventId;
    private LocalDateTime occurredAt;

}
