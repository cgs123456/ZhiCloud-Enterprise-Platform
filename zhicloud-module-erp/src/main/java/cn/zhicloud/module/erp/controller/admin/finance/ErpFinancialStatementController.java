package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpCashFlowStatementRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpIncomeStatementRespVO;
import cn.zhicloud.module.erp.service.finance.ErpFinancialStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ERP 单体财务报表 Controller（P0-4）
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 财务报表")
@RestController
@RequestMapping("/erp/financial-statement")
@Validated
public class ErpFinancialStatementController {

    @Resource
    private ErpFinancialStatementService financialStatementService;

    @GetMapping("/balance-sheet")
    @Operation(summary = "生成资产负债表")
    @Parameter(name = "asOfDate", description = "报表日期", required = true, example = "2026-07-31")
    @Parameter(name = "accountSetId", description = "FMS 账套编号（可空；按账套视角出报表，见 FMS/ERP 融合）", example = "1")
    @PreAuthorize("@ss.hasPermission('erp:financial-statement:query')")
    public CommonResult<ErpBalanceSheetRespVO> generateBalanceSheet(
            @RequestParam(value = "accountSetId", required = false) Long accountSetId,
            @RequestParam("asOfDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        return success(financialStatementService.generateBalanceSheet(accountSetId, asOfDate));
    }

    @GetMapping("/income-statement")
    @Operation(summary = "生成利润表")
    @Parameter(name = "accountSetId", description = "FMS 账套编号（可空；按账套视角出报表，见 FMS/ERP 融合）", example = "1")
    @PreAuthorize("@ss.hasPermission('erp:financial-statement:query')")
    public CommonResult<ErpIncomeStatementRespVO> generateIncomeStatement(
            @RequestParam(value = "accountSetId", required = false) Long accountSetId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(financialStatementService.generateIncomeStatement(accountSetId, startDate, endDate));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "生成现金流量表")
    @Parameter(name = "accountSetId", description = "FMS 账套编号（可空；按账套视角出报表，见 FMS/ERP 融合）", example = "1")
    @PreAuthorize("@ss.hasPermission('erp:financial-statement:query')")
    public CommonResult<ErpCashFlowStatementRespVO> generateCashFlowStatement(
            @RequestParam(value = "accountSetId", required = false) Long accountSetId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(financialStatementService.generateCashFlowStatement(accountSetId, startDate, endDate));
    }

}