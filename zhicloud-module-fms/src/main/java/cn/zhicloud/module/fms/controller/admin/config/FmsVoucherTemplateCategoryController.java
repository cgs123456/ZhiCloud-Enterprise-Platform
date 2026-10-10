package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateCategoryRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateCategorySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateCategoryDO;
import cn.zhicloud.module.fms.service.config.FmsVoucherTemplateCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FMS 凭证模板分类")
@RestController
@RequestMapping("/fms/config/voucher-template-category")
@Validated
public class FmsVoucherTemplateCategoryController {

    @Resource
    private FmsVoucherTemplateCategoryService voucherTemplateCategoryService;

    @GetMapping("/list")
    @Operation(summary = "获得凭证模板分类列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template-category:query')")
    public CommonResult<List<FmsVoucherTemplateCategoryRespVO>> getVoucherTemplateCategoryList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherTemplateCategoryDO> list = voucherTemplateCategoryService.getVoucherTemplateCategoryList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherTemplateCategoryRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得凭证模板分类精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template-category:query')")
    public CommonResult<List<FmsVoucherTemplateCategoryRespVO>> getVoucherTemplateCategorySimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherTemplateCategoryDO> list = voucherTemplateCategoryService.getVoucherTemplateCategoryList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherTemplateCategoryRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建凭证模板分类")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template-category:create')")
    public CommonResult<Long> createVoucherTemplateCategory(@Valid @RequestBody FmsVoucherTemplateCategorySaveReqVO createReqVO) {
        return success(voucherTemplateCategoryService.createVoucherTemplateCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新凭证模板分类")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template-category:update')")
    public CommonResult<Boolean> updateVoucherTemplateCategory(@Valid @RequestBody FmsVoucherTemplateCategorySaveReqVO updateReqVO) {
        voucherTemplateCategoryService.updateVoucherTemplateCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证模板分类")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template-category:delete')")
    public CommonResult<Boolean> deleteVoucherTemplateCategory(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        voucherTemplateCategoryService.deleteVoucherTemplateCategory(accountSetId, id);
        return success(true);
    }

}
