package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FMS 账套视角 - 利润表 Response VO（只读）
 *
 * <p>FMS/ERP 财务融合（台账 §12.214）：FMS 账套配置层按账套视角出三大报表，
 * 数据全部来自 ERP 已审核凭证（经 erp.api 边界包只读查询）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 账套视角利润表")
@Data
public class FmsIncomeStatementRespVO {

    @Schema(description = "起始日期", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-01-31")
    private LocalDate endDate;

    @Schema(description = "收入合计", example = "100000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "成本合计", example = "60000.00")
    private BigDecimal totalCost;

    @Schema(description = "费用合计", example = "20000.00")
    private BigDecimal totalExpense;

    @Schema(description = "净利润", example = "20000.00")
    private BigDecimal netProfit;

    @Schema(description = "收入项目明细")
    private List<FmsFinancialStatementItemVO> revenueItems;

    @Schema(description = "成本项目明细")
    private List<FmsFinancialStatementItemVO> costItems;

    @Schema(description = "费用项目明细")
    private List<FmsFinancialStatementItemVO> expenseItems;

}
