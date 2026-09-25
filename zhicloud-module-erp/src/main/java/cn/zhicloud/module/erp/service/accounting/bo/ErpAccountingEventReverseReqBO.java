package cn.zhicloud.module.erp.service.accounting.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 会计事件冲销请求。
 *
 * <p>冲销请求只描述原始事件来源与冲销会计日期；金额、币种和汇率从原始事件读取，
 * 避免调用方篡改金额导致原事件与冲销事件不一致。
 *
 * @author 智云
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountingEventReverseReqBO {

    private String eventType;
    private String sourceDocType;
    private Long sourceDocId;
    private Long sourceDocLineId;
    private Integer sourceRevision;
    private LocalDate eventDate;
    private LocalDate accountingDate;
    private LocalDateTime occurredAt;

}