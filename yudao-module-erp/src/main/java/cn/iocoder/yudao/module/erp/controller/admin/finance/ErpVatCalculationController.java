package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.finance.tax.ErpVatCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 增值税计算")
@RestController
@RequestMapping("/erp/vat-calculation")
@Validated
public class ErpVatCalculationController {

    @Resource
    private ErpVatCalculationService vatCalculationService;

    @GetMapping("/output-tax")
    @Operation(summary = "计算销项税额")
    @PreAuthorize("@ss.hasPermission('erp:vat-calculation:query')")
    public CommonResult<BigDecimal> calculateOutputTax(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(vatCalculationService.calculateOutputTax(startDate, endDate));
    }

    @GetMapping("/input-tax")
    @Operation(summary = "计算进项税额")
    @PreAuthorize("@ss.hasPermission('erp:vat-calculation:query')")
    public CommonResult<BigDecimal> calculateInputTax(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(vatCalculationService.calculateInputTax(startDate, endDate));
    }

    @GetMapping("/payable-tax")
    @Operation(summary = "计算应纳增值税额", description = "应纳税额 = 销项税额 - 进项税额")
    @PreAuthorize("@ss.hasPermission('erp:vat-calculation:query')")
    public CommonResult<Map<String, Object>> calculatePayableTax(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        BigDecimal outputTax = vatCalculationService.calculateOutputTax(startDate, endDate);
        BigDecimal inputTax = vatCalculationService.calculateInputTax(startDate, endDate);
        BigDecimal payableTax = vatCalculationService.calculatePayableTax(startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("outputTax", outputTax);
        result.put("inputTax", inputTax);
        result.put("payableTax", payableTax);
        result.put("hasRefund", payableTax.compareTo(BigDecimal.ZERO) < 0);
        return success(result);
    }

}
