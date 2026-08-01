package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxRateDO;
import cn.iocoder.yudao.module.erp.service.finance.tax.ErpTaxRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 税率")
@RestController
@RequestMapping("/erp/tax-rate")
@Validated
public class ErpTaxRateController {

    @Resource
    private ErpTaxRateService taxRateService;

    @PostMapping("/create")
    @Operation(summary = "创建税率")
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:create')")
    public CommonResult<Long> createTaxRate(@Valid @RequestBody ErpTaxRateSaveReqVO createReqVO) {
        return success(taxRateService.createTaxRate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新税率")
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:update')")
    public CommonResult<Boolean> updateTaxRate(@Valid @RequestBody ErpTaxRateSaveReqVO updateReqVO) {
        taxRateService.updateTaxRate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除税率")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:delete')")
    public CommonResult<Boolean> deleteTaxRate(@RequestParam("id") Long id) {
        taxRateService.deleteTaxRate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得税率")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:query')")
    public CommonResult<ErpTaxRateRespVO> getTaxRate(@RequestParam("id") Long id) {
        ErpTaxRateDO taxRate = taxRateService.getTaxRate(id);
        return success(BeanUtils.toBean(taxRate, ErpTaxRateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得税率分页")
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:query')")
    public CommonResult<PageResult<ErpTaxRateRespVO>> getTaxRatePage(@Valid ErpTaxRatePageReqVO pageReqVO) {
        PageResult<ErpTaxRateDO> pageResult = taxRateService.getTaxRatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpTaxRateRespVO.class));
    }

    @GetMapping("/get-default")
    @Operation(summary = "获得默认税率")
    @PreAuthorize("@ss.hasPermission('erp:tax-rate:query')")
    public CommonResult<ErpTaxRateRespVO> getDefaultTaxRate() {
        ErpTaxRateDO taxRate = taxRateService.getDefaultTaxRate();
        return success(BeanUtils.toBean(taxRate, ErpTaxRateRespVO.class));
    }

}
