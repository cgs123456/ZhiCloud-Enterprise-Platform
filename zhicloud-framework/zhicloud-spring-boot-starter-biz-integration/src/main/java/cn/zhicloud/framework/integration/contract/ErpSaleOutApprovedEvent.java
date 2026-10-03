package cn.zhicloud.framework.integration.contract;

import cn.zhicloud.framework.common.util.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 销售出库审核通过事件体（V101 首条链路）
 *
 * <p>字段刻意只放「下游建单必需 + 可追溯」的最小集合，不放 ERP 内部计算结果：
 * 事件体是跨模块契约，字段越多越难演进。WMS 建出库单需要 SKU、仓库、数量与业务单号，
 * 以及用于追溯的 ERP 单据号与行信息；其余（税率、折扣、成本）属于 ERP 内部，不下传。
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
public class ErpSaleOutApprovedEvent {
    /**
     * 聚合类型：与事件类型一样是契约的一部分，消费侧按它做聚合级顺序与追溯
     */
    public static final String AGGREGATE_TYPE = "erp_sale_out";


    /** ERP 销售出库单号（下游用业务单号追溯来源） */
    private String no;

    /** ERP 销售出库主键 */
    private Long saleOutId;

    /** ERP 客户编号（可选映射：映射缺失时下游不带客户） */
    private Long customerId;

    /** 出库时间 */
    private LocalDateTime outTime;

    /** 价税合计（下游单据金额参考，不参与对账） */
    private BigDecimal totalPrice;

    /** 出库明细 */
    private List<Item> items;

    /**
     * 事件明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        /** ERP 产品编号（需经 MDM 映射为 WMS SKU） */
        private Long productId;

        /** ERP 仓库编号（需经 MDM 映射为 WMS 仓库） */
        private Long warehouseId;

        /** 出库数量 */
        private BigDecimal count;

        /** 单价 */
        private BigDecimal price;

    }

    /**
     * 事件幂等键：业务侧可推导，同一单据审核通过只产生一个 eventId
     */
    public static String eventIdOf(Long saleOutId) {
        return "erp-sale-out-" + saleOutId + "-approved";
    }

    public String toJson() {
        return JsonUtils.toJsonString(this);
    }

    public static ErpSaleOutApprovedEvent fromJson(String json) {
        return JsonUtils.parseObject(json, ErpSaleOutApprovedEvent.class);
    }

}