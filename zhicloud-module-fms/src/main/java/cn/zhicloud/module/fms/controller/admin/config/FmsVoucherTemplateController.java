package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import cn.zhicloud.module.fms.service.config.FmsVoucherTemplateService;
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

@Tag(name = "管理后台 - FMS 凭证模板")
@RestController
@RequestMapping("/fms/config/voucher-template")
@Validated
public class FmsVoucherTemplateController {

    @Resource
    private FmsVoucherTemplateService voucherTemplateService;

    @GetMapping("/list")
    @Operation(summary = "获得凭证模板列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template:query')")
    public CommonResult<List<FmsVoucherTemplateRespVO>> getVoucherTemplateList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherTemplateDO> list = voucherTemplateService.getVoucherTemplateList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherTemplateRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得凭证模板精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template:query')")
    public CommonResult<List<FmsVoucherTemplateRespVO>> getVoucherTemplateSimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherTemplateDO> list = voucherTemplateService.getVoucherTemplateList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherTemplateRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建凭证模板")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template:create')")
    public CommonResult<Long> createVoucherTemplate(@Valid @RequestBody FmsVoucherTemplateSaveReqVO createReqVO) {
        return success(voucherTemplateService.createVoucherTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新凭证模板")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template:update')")
    public CommonResult<Boolean> updateVoucherTemplate(@Valid @RequestBody FmsVoucherTemplateSaveReqVO updateReqVO) {
        voucherTemplateService.updateVoucherTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证模板")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-template:delete')")
    public CommonResult<Boolean> deleteVoucherTemplate(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        voucherTemplateService.deleteVoucherTemplate(accountSetId, id);
        return success(true);
    }

}
