package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLinePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceLineDO;
import cn.iocoder.yudao.module.erp.service.finance.tax.ErpTaxInvoiceLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 发票明细")
@RestController
@RequestMapping("/erp/tax-invoice-line")
@Validated
public class ErpTaxInvoiceLineController {

    @Resource
    private ErpTaxInvoiceLineService taxInvoiceLineService;

    @PostMapping("/create")
    @Operation(summary = "创建发票明细")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:create')")
    public CommonResult<Long> createTaxInvoiceLine(@Valid @RequestBody ErpTaxInvoiceLineSaveReqVO createReqVO) {
        return success(taxInvoiceLineService.createTaxInvoiceLine(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发票明细")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:update')")
    public CommonResult<Boolean> updateTaxInvoiceLine(@Valid @RequestBody ErpTaxInvoiceLineSaveReqVO updateReqVO) {
        taxInvoiceLineService.updateTaxInvoiceLine(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发票明细")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:delete')")
    public CommonResult<Boolean> deleteTaxInvoiceLine(@RequestParam("id") Long id) {
        taxInvoiceLineService.deleteTaxInvoiceLine(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发票明细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:query')")
    public CommonResult<ErpTaxInvoiceLineRespVO> getTaxInvoiceLine(@RequestParam("id") Long id) {
        ErpTaxInvoiceLineDO taxInvoiceLine = taxInvoiceLineService.getTaxInvoiceLine(id);
        return success(BeanUtils.toBean(taxInvoiceLine, ErpTaxInvoiceLineRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发票明细分页")
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:query')")
    public CommonResult<PageResult<ErpTaxInvoiceLineRespVO>> getTaxInvoiceLinePage(@Valid ErpTaxInvoiceLinePageReqVO pageReqVO) {
        PageResult<ErpTaxInvoiceLineDO> pageResult = taxInvoiceLineService.getTaxInvoiceLinePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpTaxInvoiceLineRespVO.class));
    }

    @GetMapping("/list-by-invoice")
    @Operation(summary = "根据发票 ID 获得明细列表")
    @Parameter(name = "invoiceId", description = "发票 ID", required = true)
    @PreAuthorize("@ss.hasPermission('erp:tax-invoice-line:query')")
    public CommonResult<List<ErpTaxInvoiceLineRespVO>> getTaxInvoiceLineListByInvoiceId(
            @RequestParam("invoiceId") Long invoiceId) {
        List<ErpTaxInvoiceLineDO> list = taxInvoiceLineService.getTaxInvoiceLineListByInvoiceId(invoiceId);
        return success(convertList(list, item -> BeanUtils.toBean(item, ErpTaxInvoiceLineRespVO.class)));
    }

}
