package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquiryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquiryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquirySaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryItemDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.purchase.inquiry.ErpPurchaseInquiryService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购询价单")
@RestController
@RequestMapping("/erp/purchase-inquiry")
@Validated
public class ErpPurchaseInquiryController {

    @Resource
    private ErpPurchaseInquiryService inquiryService;
    @Resource
    private ErpSupplierService supplierService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建询价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:create')")
    public CommonResult<Long> createInquiry(@Valid @RequestBody ErpPurchaseInquirySaveReqVO createReqVO) {
        return success(inquiryService.createInquiry(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新询价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:update')")
    public CommonResult<Boolean> updateInquiry(@Valid @RequestBody ErpPurchaseInquirySaveReqVO updateReqVO) {
        inquiryService.updateInquiry(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除询价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:delete')")
    public CommonResult<Boolean> deleteInquiry(@RequestParam("ids") List<Long> ids) {
        inquiryService.deleteInquiry(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得询价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:query')")
    public CommonResult<ErpPurchaseInquiryRespVO> getInquiry(@RequestParam("id") Long id) {
        ErpPurchaseInquiryDO inquiry = inquiryService.getInquiry(id);
        if (inquiry == null) {
            return success(null);
        }
        List<ErpPurchaseInquiryItemDO> itemList = inquiryService.getInquiryItemListByInquiryId(id);
        ErpPurchaseInquiryRespVO respVO = BeanUtils.toBean(inquiry, ErpPurchaseInquiryRespVO.class);
        respVO.setItems(BeanUtils.toBean(itemList, ErpPurchaseInquiryRespVO.Item.class));
        respVO.setSupplierNames(buildSupplierNames(inquiry.getSupplierIds()));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得询价单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:query')")
    public CommonResult<PageResult<ErpPurchaseInquiryRespVO>> getInquiryPage(@Valid ErpPurchaseInquiryPageReqVO pageReqVO) {
        PageResult<ErpPurchaseInquiryDO> pageResult = inquiryService.getInquiryPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 收集所有供应商编号
        List<Long> supplierIds = pageResult.getList().stream()
                .flatMap(i -> StrUtil.split(i.getSupplierIds(), ",").stream()
                        .map(String::trim).filter(StrUtil::isNotBlank).map(Long::parseLong))
                .distinct().collect(Collectors.toList());
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(supplierIds);
        // 创建人信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), inquiry -> Long.parseLong(inquiry.getCreator())));
        // 拼接
        return success(BeanUtils.toBean(pageResult, ErpPurchaseInquiryRespVO.class, inquiry -> {
            inquiry.setSupplierNames(buildSupplierNames(inquiry.getSupplierIds(), supplierMap));
            AdminUserRespDTO user = userMap.get(Long.parseLong(inquiry.getCreator()));
            if (user != null) {
                inquiry.setCreatorName(user.getNickname());
            }
        }));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交发布询价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:submit')")
    public CommonResult<Boolean> submitInquiry(@RequestParam("id") Long id) {
        inquiryService.submitInquiry(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭询价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:close')")
    public CommonResult<Boolean> closeInquiry(@RequestParam("id") Long id) {
        inquiryService.closeInquiry(id);
        return success(true);
    }

    @PostMapping("/convert")
    @Operation(summary = "询价单转采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-inquiry:convert')")
    public CommonResult<Long> convertToPurchaseOrder(@RequestParam("id") Long id,
                                                     @RequestParam("supplierId") Long supplierId) {
        return success(inquiryService.convertToPurchaseOrder(id, supplierId));
    }

    /**
     * 解析供应商编号列表，拼接供应商名称
     */
    private String buildSupplierNames(String supplierIds, Map<Long, ErpSupplierDO> supplierMap) {
        if (StrUtil.isBlank(supplierIds)) {
            return null;
        }
        return StrUtil.split(supplierIds, ",").stream()
                .map(String::trim).filter(StrUtil::isNotBlank).map(Long::parseLong)
                .map(id -> supplierMap.getOrDefault(id, new ErpSupplierDO().setId(id).setName("未知供应商")))
                .map(ErpSupplierDO::getName)
                .collect(Collectors.joining("，"));
    }

    private String buildSupplierNames(String supplierIds) {
        if (StrUtil.isBlank(supplierIds)) {
            return null;
        }
        List<Long> ids = StrUtil.split(supplierIds, ",").stream()
                .map(String::trim).filter(StrUtil::isNotBlank).map(Long::parseLong)
                .distinct().collect(Collectors.toList());
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(ids);
        return buildSupplierNames(supplierIds, supplierMap);
    }

}
