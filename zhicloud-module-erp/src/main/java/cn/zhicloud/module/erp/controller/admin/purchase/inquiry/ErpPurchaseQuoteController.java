package cn.zhicloud.module.erp.controller.admin.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuotePageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuoteRespVO;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuoteSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import cn.zhicloud.module.erp.service.purchase.ErpSupplierService;
import cn.zhicloud.module.erp.service.purchase.inquiry.ErpPurchaseInquiryService;
import cn.zhicloud.module.erp.service.purchase.inquiry.ErpPurchaseQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购报价单")
@RestController
@RequestMapping("/erp/purchase-quote")
@Validated
public class ErpPurchaseQuoteController {

    @Resource
    private ErpPurchaseQuoteService quoteService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseInquiryService inquiryService;

    @PostMapping("/create")
    @Operation(summary = "创建报价单（供应商报价）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-quote:create')")
    public CommonResult<Long> createQuote(@Valid @RequestBody ErpPurchaseQuoteSaveReqVO createReqVO) {
        return success(quoteService.createQuote(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-quote:update')")
    public CommonResult<Boolean> updateQuote(@Valid @RequestBody ErpPurchaseQuoteSaveReqVO updateReqVO) {
        quoteService.updateQuote(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-quote:delete')")
    public CommonResult<Boolean> deleteQuote(@RequestParam("ids") List<Long> ids) {
        quoteService.deleteQuote(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-quote:query')")
    public CommonResult<ErpPurchaseQuoteRespVO> getQuote(@RequestParam("id") Long id) {
        ErpPurchaseQuoteDO quote = quoteService.getQuote(id);
        if (quote == null) {
            return success(null);
        }
        List<ErpPurchaseQuoteItemDO> itemList = quoteService.getQuoteItemListByQuoteId(id);
        ErpPurchaseQuoteRespVO respVO = BeanUtils.toBean(quote, ErpPurchaseQuoteRespVO.class);
        respVO.setItems(BeanUtils.toBean(itemList, ErpPurchaseQuoteRespVO.Item.class));
        // 供应商名称
        ErpSupplierDO supplier = supplierService.getSupplier(quote.getSupplierId());
        if (supplier != null) {
            respVO.setSupplierName(supplier.getName());
        }
        // 询价单号
        ErpPurchaseInquiryDO inquiry = inquiryService.getInquiry(quote.getInquiryId());
        if (inquiry != null) {
            respVO.setInquiryNo(inquiry.getNo());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得报价单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-quote:query')")
    public CommonResult<PageResult<ErpPurchaseQuoteRespVO>> getQuotePage(@Valid ErpPurchaseQuotePageReqVO pageReqVO) {
        PageResult<ErpPurchaseQuoteDO> pageResult = quoteService.getQuotePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 供应商信息
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseQuoteDO::getSupplierId));
        // 询价单信息
        Map<Long, ErpPurchaseInquiryDO> inquiryMap = convertSet(pageResult.getList(), ErpPurchaseQuoteDO::getInquiryId)
                .stream().collect(java.util.stream.Collectors.toMap(id -> id,
                        id -> inquiryService.getInquiry(id), (a, b) -> a));
        return success(BeanUtils.toBean(pageResult, ErpPurchaseQuoteRespVO.class, quote -> {
            ErpSupplierDO supplier = supplierMap.get(quote.getSupplierId());
            if (supplier != null) {
                quote.setSupplierName(supplier.getName());
            }
            ErpPurchaseInquiryDO inquiry = inquiryMap.get(quote.getInquiryId());
            if (inquiry != null) {
                quote.setInquiryNo(inquiry.getNo());
            }
        }));
    }

}
