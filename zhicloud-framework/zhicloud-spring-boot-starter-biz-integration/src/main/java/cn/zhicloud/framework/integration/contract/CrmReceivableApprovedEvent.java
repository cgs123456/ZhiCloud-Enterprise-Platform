package cn.zhicloud.framework.integration.contract;

import cn.zhicloud.framework.common.util.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CRM 回款审批通过事件体（D1 首条链路，§12.267）
 *
 * <p>字段刻意只放「下游建单必需 + 可追溯」的最小集合，不放 CRM 内部实现：
 * ERP 建收款单需要客户、回款日期与金额，以及用于追溯的 CRM 回款编号与主键；
 * 合同、回款方式、归属人属于 CRM 内部，不下传；账户与财务人员维度 CRM 域没有，
 * 由 ERP 财务在审核过账前补选。
 *
 * <p>{@code eventId} 由 {@link #eventIdOf} 从业务主键推导，不用随机数也不用自增主键：
 * 同一业务动作重复触发时能得到相同 eventId，消费侧才能靠 Inbox 唯一键去重。
 *
 * @author 智云集成治理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmReceivableApprovedEvent {

    /**
     * 聚合类型：与事件类型一样是契约的一部分，消费侧按它做聚合级追溯
     */
    public static final String AGGREGATE_TYPE = "crm_receivable";

    /** CRM 回款编号（下游用业务单号追溯来源） */
    private String no;

    /** CRM 回款主键 */
    private Long receivableId;

    /** CRM 客户编号（需经 MDM 映射为 ERP 客户） */
    private Long customerId;

    /** 回款日期（作为 ERP 收款时间） */
    private LocalDateTime returnTime;

    /** 回款金额（元） */
    private BigDecimal price;

    /**
     * 事件幂等键：业务侧可推导，同一回款审批通过只产生一个 eventId
     */
    public static String eventIdOf(Long receivableId) {
        return "crm-receivable-" + receivableId + "-approved";
    }

    public String toJson() {
        return JsonUtils.toJsonString(this);
    }

    public static CrmReceivableApprovedEvent fromJson(String json) {
        return JsonUtils.parseObject(json, CrmReceivableApprovedEvent.class);
    }

}