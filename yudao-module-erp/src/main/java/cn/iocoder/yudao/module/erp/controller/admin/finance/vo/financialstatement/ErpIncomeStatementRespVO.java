package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 利润表 Response VO")
@Data
public class ErpIncomeStatementRespVO {

    @Schema(description = "起始日期", example = "2026-07-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-07-31")
    private LocalDate endDate;

    @Schema(description = "营业收入合计", example = "80000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "营业成本合计", example = "30000.00")
    private BigDecimal totalCost;

    @Schema(description = "期间费用合计", example = "10000.00")
    private BigDecimal totalExpense;

    @Schema(description = "净利润（收入-成本-费用）", example = "40000.00")
    private BigDecimal netProfit;

    @Schema(description = "收入项目明细")
    private List<ErpFinancialStatementItemVO> revenueItems;

    @Schema(description = "成本项目明细")
    private List<ErpFinancialStatementItemVO> costItems;

    @Schema(description = "费用项目明细")
    private List<ErpFinancialStatementItemVO> expenseItems;

}