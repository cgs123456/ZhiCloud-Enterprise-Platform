package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FMS 账套视角 - 现金流量表 Response VO（只读）
 *
 * <p>FMS/ERP 财务融合（台账 §12.214）：FMS 账套配置层按账套视角出三大报表，
 * 数据全部来自 ERP 已审核凭证（经 erp.api 边界包只读查询）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 账套视角现金流量表")
@Data
public class FmsCashFlowStatementRespVO {

    @Schema(description = "起始日期", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-01-31")
    private LocalDate endDate;

    @Schema(description = "数据来源（CASH_FLOW 银行流水 / VOUCHER 凭证兜底）", example = "VOUCHER")
    private String dataSource;

    @Schema(description = "经营活动净流量", example = "10000.00")
    private BigDecimal netOperatingCashFlow;

    @Schema(description = "投资活动净流量", example = "-5000.00")
    private BigDecimal netInvestingCashFlow;

    @Schema(description = "筹资活动净流量", example = "0.00")
    private BigDecimal netFinancingCashFlow;

    @Schema(description = "现金净增加额", example = "5000.00")
    private BigDecimal netCashFlow;

    @Schema(description = "经营活动项目明细")
    private List<FmsFinancialStatementItemVO> operatingItems;

    @Schema(description = "投资项目明细")
    private List<FmsFinancialStatementItemVO> investingItems;

    @Schema(description = "筹资项目明细")
    private List<FmsFinancialStatementItemVO> financingItems;

}
