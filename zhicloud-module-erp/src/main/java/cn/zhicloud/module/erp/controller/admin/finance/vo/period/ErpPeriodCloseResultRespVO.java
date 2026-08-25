package cn.zhicloud.module.erp.controller.admin.finance.vo.period;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 期末处理执行结果 Response VO（P0-6）
 *
 * <p>月末检查/调汇/损益结转三种类型共用，按需填充字段。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 期末处理执行结果 Response VO")
@Data
public class ErpPeriodCloseResultRespVO {

    @Schema(description = "期间编号")
    private Long periodId;

    @Schema(description = "期间编码")
    private String periodCode;

    @Schema(description = "处理类型（10 月末检查 / 20 调汇 / 30 损益结转）")
    private Integer type;

    @Schema(description = "处理状态（10 成功 / 20 跳过 / 30 失败）")
    private Integer processStatus;

    @Schema(description = "执行人")
    private String executedBy;

    @Schema(description = "执行时间")
    private LocalDateTime executedTime;

    // === 月末检查字段 ===

    @Schema(description = "未审核采购入库单数")
    private Integer unapprovedPurchaseInCount;

    @Schema(description = "未审核销售出库单数")
    private Integer unapprovedSaleOutCount;

    @Schema(description = "未审核付款单数")
    private Integer unapprovedPaymentCount;

    @Schema(description = "未审核收款单数")
    private Integer unapprovedReceiptCount;

    @Schema(description = "未审核其他入库单数")
    private Integer unapprovedStockInCount;

    @Schema(description = "未审核其他出库单数")
    private Integer unapprovedStockOutCount;

    // === 调汇字段 ===

    @Schema(description = "调整账户数")
    private Integer adjustedAccountCount;

    @Schema(description = "调整金额（正数为收益，负数为损失）")
    private BigDecimal adjustmentAmount;

    // === 损益结转字段 ===

    @Schema(description = "销售收入合计")
    private BigDecimal totalRevenue;

    @Schema(description = "采购支出合计")
    private BigDecimal totalExpense;

    @Schema(description = "本期净利润 = 收入 - 支出")
    private BigDecimal netProfit;

    @Schema(description = "原始摘要 JSON")
    private String summary;

}
