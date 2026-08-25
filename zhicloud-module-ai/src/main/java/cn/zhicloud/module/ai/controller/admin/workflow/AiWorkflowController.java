package cn.zhicloud.module.ai.controller.admin.workflow;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.*;
import cn.zhicloud.module.ai.dal.dataobject.workflow.AiWorkflowDO;
import cn.zhicloud.module.ai.service.workflow.AiWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 工作流")
@RestController
@RequestMapping("/ai/workflow")
@Slf4j
public class AiWorkflowController {

    @Resource
    private AiWorkflowService workflowService;

    @PostMapping("/create")
    @Operation(summary = "创建 AI 工作流")
    @PreAuthorize("@ss.hasPermission('ai:workflow:create')")
    public CommonResult<Long> createWorkflow(@Valid @RequestBody AiWorkflowSaveReqVO createReqVO) {
        return success(workflowService.createWorkflow(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 AI 工作流")
    @PreAuthorize("@ss.hasPermission('ai:workflow:update')")
    public CommonResult<Boolean> updateWorkflow(@Valid @RequestBody AiWorkflowSaveReqVO updateReqVO) {
        workflowService.updateWorkflow(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 AI 工作流")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:workflow:delete')")
    public CommonResult<Boolean> deleteWorkflow(@RequestParam("id") Long id) {
        workflowService.deleteWorkflow(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 AI 工作流")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<AiWorkflowRespVO> getWorkflow(@RequestParam("id") Long id) {
        AiWorkflowDO workflow = workflowService.getWorkflow(id);
        return success(BeanUtils.toBean(workflow, AiWorkflowRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 AI 工作流分页")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<PageResult<AiWorkflowRespVO>> getWorkflowPage(@Valid AiWorkflowPageReqVO pageReqVO) {
        PageResult<AiWorkflowDO> pageResult = workflowService.getWorkflowPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiWorkflowRespVO.class));
    }

    @PostMapping("/test")
    @Operation(summary = "测试 AI 工作流")
    @PreAuthorize("@ss.hasPermission('ai:workflow:test')")
    public CommonResult<Object> testWorkflow(@Valid @RequestBody AiWorkflowTestReqVO testReqVO) {
        return success(workflowService.testWorkflow(testReqVO));
    }

    @GetMapping("/node-types")
    @Operation(summary = "获取所有可用节点类型列表")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<List<AiWorkflowNodeTypeRespVO>> getNodeTypeList() {
        return success(workflowService.getNodeTypeList());
    }

    @GetMapping("/node-config-schema")
    @Operation(summary = "获取节点配置 Schema（前端表单渲染用）")
    @Parameter(name = "nodeType", description = "节点类型标识", required = true, example = "llmNode")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<AiWorkflowNodeTypeRespVO> getNodeConfigSchema(@RequestParam("nodeType") String nodeType) {
        return success(workflowService.getNodeConfigSchema(nodeType));
    }

    @PostMapping("/validate")
    @Operation(summary = "校验工作流 JSON 配置合法性")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<AiWorkflowValidateRespVO> validateWorkflow(@Valid @RequestBody AiWorkflowValidateReqVO reqVO) {
        return success(workflowService.validateWorkflow(reqVO));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取工作流模板列表")
    @PreAuthorize("@ss.hasPermission('ai:workflow:query')")
    public CommonResult<List<AiWorkflowTemplateRespVO>> getWorkflowTemplateList() {
        return success(workflowService.getWorkflowTemplateList());
    }

}
