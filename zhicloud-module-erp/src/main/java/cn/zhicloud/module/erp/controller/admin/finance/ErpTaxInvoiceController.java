package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoicePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoiceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoiceSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import cn.zhicloud.module.erp.service.finance.tax.ErpTaxInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 发票")
@RestController
@RequestMapping("/erp/tax-invoice")
@Validated
public class ErpTaxInvoiceController {

    @Resource
    private ErpTaxInvoiceService taxInvoiceService;

    @PostMapping("/create")
    @Operation(summary = "创建发票（草稿）")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:create')")
    public CommonResult<Long> createTaxInvoice(@Valid @RequestBody ErpTaxInvoiceSaveReqVO createReqVO) {
        return success(taxInvoiceService.createTaxInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发票")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:update')")
    public CommonResult<Boolean> updateTaxInvoice(@Valid @RequestBody ErpTaxInvoiceSaveReqVO updateReqVO) {
        taxInvoiceService.updateTaxInvoice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发票")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:delete')")
    public CommonResult<Boolean> deleteTaxInvoice(@RequestParam("id") Long id) {
        taxInvoiceService.deleteTaxInvoice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发票")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:query')")
    public CommonResult<ErpTaxInvoiceRespVO> getTaxInvoice(@RequestParam("id") Long id) {
        ErpTaxInvoiceDO taxInvoice = taxInvoiceService.getTaxInvoice(id);
        return success(BeanUtils.toBean(taxInvoice, ErpTaxInvoiceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发票分页")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:query')")
    public CommonResult<PageResult<ErpTaxInvoiceRespVO>> getTaxInvoicePage(@Valid ErpTaxInvoicePageReqVO pageReqVO) {
        PageResult<ErpTaxInvoiceDO> pageResult = taxInvoiceService.getTaxInvoicePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpTaxInvoiceRespVO.class));
    }

    @PostMapping("/issue")
    @Operation(summary = "开具发票（草稿 → 已开具）", description = "调用金税四期接口")
    @Parameter(name = "id", description = "发票编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:issue')")
    public CommonResult<Boolean> issueInvoice(@RequestParam("id") Long id) {
        taxInvoiceService.issueInvoice(id);
        return success(true);
    }

    @PostMapping("/revoke")
    @Operation(summary = "作废发票（已开具 → 已作废）", description = "调用金税四期接口")
    @Parameter(name = "id", description = "发票编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:revoke')")
    public CommonResult<Boolean> revokeInvoice(@RequestParam("id") Long id) {
        taxInvoiceService.revokeInvoice(id);
        return success(true);
    }

    @PostMapping("/red")
    @Operation(summary = "红冲发票（已开具 → 已红冲）", description = "调用金税四期接口")
    @Parameter(name = "id", description = "发票编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice:red')")
    public CommonResult<Boolean> redInvoice(@RequestParam("id") Long id) {
        taxInvoiceService.redInvoice(id);
        return success(true);
    }

}
