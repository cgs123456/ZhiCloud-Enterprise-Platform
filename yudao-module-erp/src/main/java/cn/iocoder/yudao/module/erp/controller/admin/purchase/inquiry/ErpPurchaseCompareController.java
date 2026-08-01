package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseComparePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseCompareRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseCompareSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareLineDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.purchase.inquiry.ErpPurchaseCompareService;
import cn.iocoder.yudao.module.erp.service.purchase.inquiry.ErpPurchaseInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购比价单")
@RestController
@RequestMapping("/erp/purchase-compare")
@Validated
public class ErpPurchaseCompareController {

    @Resource
    private ErpPurchaseCompareService compareService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseInquiryService inquiryService;

    @PostMapping("/generate")
    @Operation(summary = "生成比价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-compare:generate')")
    public CommonResult<Long> generateCompare(@Valid @RequestBody ErpPurchaseCompareSaveReqVO createReqVO) {
        return success(compareService.generateCompare(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得比价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-compare:query')")
    public CommonResult<ErpPurchaseCompareRespVO> getCompare(@RequestParam("id") Long id) {
        ErpPurchaseCompareDO compare = compareService.getCompare(id);
        if (compare == null) {
            return success(null);
        }
        List<ErpPurchaseCompareLineDO> lineList = compareService.getCompareLineListByCompareId(id);
        ErpPurchaseCompareRespVO respVO = BeanUtils.toBean(compare, ErpPurchaseCompareRespVO.class);
        respVO.setLines(BeanUtils.toBean(lineList, ErpPurchaseCompareRespVO.Line.class));
        // 供应商名称
        fillSupplierNames(respVO);
        // 询价单号
        ErpPurchaseInquiryDO inquiry = inquiryService.getInquiry(compare.getInquiryId());
        if (inquiry != null) {
            respVO.setInquiryNo(inquiry.getNo());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得比价单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-compare:query')")
    public CommonResult<PageResult<ErpPurchaseCompareRespVO>> getComparePage(@Valid ErpPurchaseComparePageReqVO pageReqVO) {
        PageResult<ErpPurchaseCompareDO> pageResult = compareService.getComparePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 供应商信息
        List<Long> supplierIds = pageResult.getList().stream()
                .map(ErpPurchaseCompareDO::getRecommendSupplierId)
                .filter(java.util.Objects::nonNull)
                .distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(supplierIds);
        // 询价单信息
        Map<Long, ErpPurchaseInquiryDO> inquiryMap = convertSet(pageResult.getList(), ErpPurchaseCompareDO::getInquiryId)
                .stream().collect(java.util.stream.Collectors.toMap(id -> id,
                        id -> inquiryService.getInquiry(id), (a, b) -> a));
        return success(BeanUtils.toBean(pageResult, ErpPurchaseCompareRespVO.class, compare -> {
            ErpSupplierDO supplier = supplierMap.get(compare.getRecommendSupplierId());
            if (supplier != null) {
                compare.setRecommendSupplierName(supplier.getName());
            }
            ErpPurchaseInquiryDO inquiry = inquiryMap.get(compare.getInquiryId());
            if (inquiry != null) {
                compare.setInquiryNo(inquiry.getNo());
            }
        }));
    }

    /**
     * 填充比价明细行与主表的供应商名称
     */
    private void fillSupplierNames(ErpPurchaseCompareRespVO respVO) {
        Map<Long, ErpSupplierDO> supplierMap = new HashMap<>();
        // 主表推荐供应商
        if (respVO.getRecommendSupplierId() != null) {
            supplierMap.putAll(supplierService.getSupplierMap(
                    java.util.Collections.singletonList(respVO.getRecommendSupplierId())));
        }
        // 明细行供应商
        if (CollUtil.isNotEmpty(respVO.getLines())) {
            List<Long> lineSupplierIds = respVO.getLines().stream()
                    .map(ErpPurchaseCompareRespVO.Line::getSupplierId)
                    .filter(java.util.Objects::nonNull)
                    .distinct().collect(java.util.stream.Collectors.toList());
            if (CollUtil.isNotEmpty(lineSupplierIds)) {
                supplierMap.putAll(supplierService.getSupplierMap(lineSupplierIds));
            }
        }
        ErpSupplierDO recommendSupplier = supplierMap.get(respVO.getRecommendSupplierId());
        if (recommendSupplier != null) {
            respVO.setRecommendSupplierName(recommendSupplier.getName());
        }
        if (respVO.getLines() != null) {
            respVO.getLines().forEach(line -> {
                ErpSupplierDO supplier = supplierMap.get(line.getSupplierId());
                if (supplier != null) {
                    line.setSupplierName(supplier.getName());
                }
            });
        }
    }

}
