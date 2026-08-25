package cn.zhicloud.module.oa.controller.admin.approvaltemplate;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplatePageReqVO;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplateRespVO;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplateSaveReqVO;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplateSimpleRespVO;
import cn.zhicloud.module.oa.dal.dataobject.approvaltemplate.OaApprovalTemplateDO;
import cn.zhicloud.module.oa.service.approvaltemplate.OaApprovalTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 审批模板库")
@RestController
@RequestMapping("/oa/approval-template")
@Validated
public class OaApprovalTemplateController {

    @Resource
    private OaApprovalTemplateService approvalTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建审批模板")
    @PreAuthorize("@ss.hasPermission('oa:approval-template:create')")
    public CommonResult<Long> createApprovalTemplate(@Valid @RequestBody OaApprovalTemplateSaveReqVO createReqVO) {
        return success(approvalTemplateService.createApprovalTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新审批模板")
    @PreAuthorize("@ss.hasPermission('oa:approval-template:update')")
    public CommonResult<Boolean> updateApprovalTemplate(@Valid @RequestBody OaApprovalTemplateSaveReqVO updateReqVO) {
        approvalTemplateService.updateApprovalTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除审批模板")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:approval-template:delete')")
    public CommonResult<Boolean> deleteApprovalTemplate(@RequestParam("id") Long id) {
        approvalTemplateService.deleteApprovalTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得审批模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:approval-template:query')")
    public CommonResult<OaApprovalTemplateRespVO> getApprovalTemplate(@RequestParam("id") Long id) {
        OaApprovalTemplateDO approvalTemplate = approvalTemplateService.getApprovalTemplate(id);
        return success(BeanUtils.toBean(approvalTemplate, OaApprovalTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得审批模板分页")
    @PreAuthorize("@ss.hasPermission('oa:approval-template:query')")
    public CommonResult<PageResult<OaApprovalTemplateRespVO>> getApprovalTemplatePage(@Valid OaApprovalTemplatePageReqVO pageReqVO) {
        PageResult<OaApprovalTemplateDO> pageResult = approvalTemplateService.getApprovalTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaApprovalTemplateRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的审批模板列表")
    @PreAuthorize("@ss.hasPermission('oa:approval-template:query')")
    public CommonResult<List<OaApprovalTemplateSimpleRespVO>> getEnabledList() {
        List<OaApprovalTemplateDO> list = approvalTemplateService.getEnabledList();
        return success(BeanUtils.toBean(list, OaApprovalTemplateSimpleRespVO.class));
    }

    @PutMapping("/increment-usage")
    @Operation(summary = "累加审批模板使用次数")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:approval-template:update')")
    public CommonResult<Boolean> incrementUsageCount(@RequestParam("id") Long id) {
        approvalTemplateService.incrementUsageCount(id);
        return success(true);
    }

}
