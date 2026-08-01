package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 汇率")
@RestController
@RequestMapping("/erp/exchange-rate")
@Validated
public class ErpExchangeRateController {

    @Resource
    private ErpExchangeRateService exchangeRateService;

    @PostMapping("/create")
    @Operation(summary = "创建汇率")
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:create')")
    public CommonResult<Long> createExchangeRate(@Valid @RequestBody ErpExchangeRateSaveReqVO createReqVO) {
        return success(exchangeRateService.createExchangeRate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新汇率")
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:update')")
    public CommonResult<Boolean> updateExchangeRate(@Valid @RequestBody ErpExchangeRateSaveReqVO updateReqVO) {
        exchangeRateService.updateExchangeRate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除汇率")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:delete')")
    public CommonResult<Boolean> deleteExchangeRate(@RequestParam("id") Long id) {
        exchangeRateService.deleteExchangeRate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得汇率")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:query')")
    public CommonResult<ErpExchangeRateRespVO> getExchangeRate(@RequestParam("id") Long id) {
        ErpExchangeRateDO rate = exchangeRateService.getExchangeRate(id);
        return success(BeanUtils.toBean(rate, ErpExchangeRateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得汇率分页")
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:query')")
    public CommonResult<PageResult<ErpExchangeRateRespVO>> getExchangeRatePage(@Valid ErpExchangeRatePageReqVO pageReqVO) {
        PageResult<ErpExchangeRateDO> pageResult = exchangeRateService.getExchangeRatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpExchangeRateRespVO.class));
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新汇率", description = "根据源币种、目标币种与日期查询最新的有效汇率")
    @Parameters({
            @Parameter(name = "fromCurrencyId", description = "源币种编号", required = true),
            @Parameter(name = "toCurrencyId", description = "目标币种编号", required = true),
            @Parameter(name = "date", description = "查询日期（默认今天）", example = "2026-07-29")
    })
    @PreAuthorize("@ss.hasPermission('erp:exchange-rate:query')")
    public CommonResult<ErpExchangeRateRespVO> getLatestRate(
            @RequestParam("fromCurrencyId") Long fromCurrencyId,
            @RequestParam("toCurrencyId") Long toCurrencyId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        ErpExchangeRateDO rate = exchangeRateService.getLatestRate(fromCurrencyId, toCurrencyId, date);
        return success(BeanUtils.toBean(rate, ErpExchangeRateRespVO.class));
    }

}
