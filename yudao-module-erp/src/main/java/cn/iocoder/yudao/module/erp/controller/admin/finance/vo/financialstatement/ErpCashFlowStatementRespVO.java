package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 现金流量表 Response VO")
@Data
public class ErpCashFlowStatementRespVO {

    @Schema(description = "起始日期", example = "2026-07-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-07-31")
    private LocalDate endDate;

    @Schema(description = "经营活动现金流量净额", example = "20000.00")
    private BigDecimal netOperatingCashFlow;

    @Schema(description = "投资活动现金流量净额", example = "-5000.00")
    private BigDecimal netInvestingCashFlow;

    @Schema(description = "筹资活动现金流量净额", example = "0.00")
    private BigDecimal netFinancingCashFlow;

    @Schema(description = "现金及现金等价物净增加额", example = "15000.00")
    private BigDecimal netCashFlow;

    @Schema(description = "经营活动现金流明细")
    private List<ErpFinancialStatementItemVO> operatingItems;

    @Schema(description = "投资活动现金流明细")
    private List<ErpFinancialStatementItemVO> investingItems;

    @Schema(description = "筹资活动现金流明细")
    private List<ErpFinancialStatementItemVO> financingItems;

    @Schema(description = "数据来源（CASH_FLOW=银行流水；VOUCHER=凭证银行科目变动）", example = "CASH_FLOW")
    private String dataSource;

}