package cn.iocoder.yudao.module.ai.controller.admin.prompt;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplateRenderReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplateRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplateSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.prompt.AiPromptTemplateDO;
import cn.iocoder.yudao.module.ai.service.prompt.AiPromptTemplateService;
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

@Tag(name = "管理后台 - AI Prompt 模板")
@RestController
@RequestMapping("/ai/prompt-template")
@Validated
public class AiPromptTemplateController {

    @Resource
    private AiPromptTemplateService promptTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建 Prompt 模板")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:create')")
    public CommonResult<Long> createPromptTemplate(@RequestBody @Valid AiPromptTemplateSaveReqVO createReqVO) {
        return success(promptTemplateService.createPromptTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 Prompt 模板")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:update')")
    public CommonResult<Boolean> updatePromptTemplate(@RequestBody @Valid AiPromptTemplateSaveReqVO updateReqVO) {
        promptTemplateService.updatePromptTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 Prompt 模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:delete')")
    public CommonResult<Boolean> deletePromptTemplate(@RequestParam("id") Long id) {
        promptTemplateService.deletePromptTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 Prompt 模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:query')")
    public CommonResult<AiPromptTemplateRespVO> getPromptTemplate(@RequestParam("id") Long id) {
        AiPromptTemplateDO template = promptTemplateService.getPromptTemplate(id);
        return success(BeanUtils.toBean(template, AiPromptTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取 Prompt 模板分页")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:query')")
    public CommonResult<PageResult<AiPromptTemplateRespVO>> getPromptTemplatePage(
            @Valid AiPromptTemplatePageReqVO pageReqVO) {
        PageResult<AiPromptTemplateDO> pageResult = promptTemplateService.getPromptTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiPromptTemplateRespVO.class));
    }

    @PostMapping("/render")
    @Operation(summary = "渲染 Prompt 模板")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:query')")
    public CommonResult<String> renderPromptTemplate(@RequestBody @Valid AiPromptTemplateRenderReqVO reqVO) {
        return success(promptTemplateService.renderTemplate(reqVO.getCode(), reqVO.getVariables()));
    }

    @GetMapping("/by-category/{category}")
    @Operation(summary = "按分类查询 Prompt 模板")
    @Parameter(name = "category", description = "分类", required = true, example = "RAG")
    @PreAuthorize("@ss.hasPermission('ai:prompt-template:query')")
    public CommonResult<List<AiPromptTemplateRespVO>> getPromptTemplateByCategory(
            @PathVariable("category") String category) {
        List<AiPromptTemplateDO> list = promptTemplateService.getTemplatesByCategory(category);
        return success(BeanUtils.toBean(list, AiPromptTemplateRespVO.class));
    }

}
