package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FMS 账套视角 - 资产负债表 Response VO（只读）
 *
 * <p>FMS/ERP 财务融合（台账 §12.214）：FMS 账套配置层按账套视角出三大报表，
 * 数据全部来自 ERP 已审核凭证（经 erp.api 边界包只读查询）。
 *
 * <p>刻意做成独立 VO 而非复用 ERP 的 ErpBalanceSheetRespVO：跨模块 VO 泄漏会让
 * ERP 侧字段变化直接改到 FMS 的接口契约，边界处独立转换（CLAUDE.md 边界纪律）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 账套视角资产负债表")
@Data
public class FmsBalanceSheetRespVO {

    @Schema(description = "报表日期", example = "2026-07-31")
    private LocalDate asOfDate;

    @Schema(description = "资产合计", example = "100000.00")
    private BigDecimal totalAssets;

    @Schema(description = "负债合计", example = "40000.00")
    private BigDecimal totalLiabilities;

    @Schema(description = "所有者权益合计", example = "60000.00")
    private BigDecimal totalEquity;

    @Schema(description = "资产项目明细")
    private List<FmsFinancialStatementItemVO> assetItems;

    @Schema(description = "负债项目明细")
    private List<FmsFinancialStatementItemVO> liabilityItems;

    @Schema(description = "所有者权益项目明细")
    private List<FmsFinancialStatementItemVO> equityItems;

}
