package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产负债表 Response VO")
@Data
public class ErpBalanceSheetRespVO {

    @Schema(description = "报表日期", example = "2026-07-31")
    private LocalDate asOfDate;

    @Schema(description = "资产合计", example = "100000.00")
    private BigDecimal totalAssets;

    @Schema(description = "负债合计", example = "40000.00")
    private BigDecimal totalLiabilities;

    @Schema(description = "所有者权益合计", example = "60000.00")
    private BigDecimal totalEquity;

    @Schema(description = "资产项目明细")
    private List<ErpFinancialStatementItemVO> assetItems;

    @Schema(description = "负债项目明细")
    private List<ErpFinancialStatementItemVO> liabilityItems;

    @Schema(description = "所有者权益项目明细")
    private List<ErpFinancialStatementItemVO> equityItems;

}