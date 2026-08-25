package cn.zhicloud.module.qms.controller.admin.audit;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.audit.vo.*;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanAuditorDO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanDO;
import cn.zhicloud.module.qms.service.audit.QmsAuditPlanService;
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

/**
 * QMS 审核计划 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - QMS 审核计划")
@RestController
@RequestMapping("/qms/audit-plan")
@Validated
public class QmsAuditPlanController {

    @Resource
    private QmsAuditPlanService auditPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建审核计划")
    @PreAuthorize("@ss.hasPermission('qms:audit:create')")
    public CommonResult<Long> createAuditPlan(@Valid @RequestBody QmsAuditPlanSaveReqVO createReqVO) {
        return success(auditPlanService.createAuditPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新审核计划")
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Boolean> updateAuditPlan(@Valid @RequestBody QmsAuditPlanSaveReqVO updateReqVO) {
        auditPlanService.updateAuditPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除审核计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:audit:delete')")
    public CommonResult<Boolean> deleteAuditPlan(@RequestParam("id") Long id) {
        auditPlanService.deleteAuditPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得审核计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<QmsAuditPlanRespVO> getAuditPlan(@RequestParam("id") Long id) {
        QmsAuditPlanDO auditPlan = auditPlanService.getAuditPlan(id);
        return success(BeanUtils.toBean(auditPlan, QmsAuditPlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得审核计划分页")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<PageResult<QmsAuditPlanRespVO>> getAuditPlanPage(@Valid QmsAuditPlanPageReqVO pageReqVO) {
        PageResult<QmsAuditPlanDO> pageResult = auditPlanService.getAuditPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsAuditPlanRespVO.class));
    }

    @PutMapping("/execute")
    @Operation(summary = "开始执行审核", description = "仅在状态为「已计划」时允许，流转为「已执行」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:execute')")
    public CommonResult<Boolean> executeAuditPlan(@RequestParam("id") Long id) {
        auditPlanService.executeAuditPlan(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成审核", description = "仅在状态为「已执行」时允许，流转为「已完成」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:execute')")
    public CommonResult<Boolean> completeAuditPlan(@RequestParam("id") Long id) {
        auditPlanService.completeAuditPlan(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消审核", description = "仅在状态为「已计划」或「已执行」时允许，流转为「已取消」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:execute')")
    public CommonResult<Boolean> cancelAuditPlan(@RequestParam("id") Long id) {
        auditPlanService.cancelAuditPlan(id);
        return success(true);
    }

    @PostMapping("/auditor/create")
    @Operation(summary = "添加审核组成员")
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Long> addAuditor(@Valid @RequestBody QmsAuditPlanAuditorSaveReqVO reqVO) {
        return success(auditPlanService.addAuditor(reqVO));
    }

    @PutMapping("/auditor/update")
    @Operation(summary = "更新审核组成员")
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Boolean> updateAuditor(@Valid @RequestBody QmsAuditPlanAuditorSaveReqVO reqVO) {
        auditPlanService.updateAuditor(reqVO);
        return success(true);
    }

    @DeleteMapping("/auditor/delete")
    @Operation(summary = "删除审核组成员")
    @Parameter(name = "id", description = "成员编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Boolean> deleteAuditor(@RequestParam("id") Long id) {
        auditPlanService.deleteAuditor(id);
        return success(true);
    }

    @GetMapping("/auditor/list")
    @Operation(summary = "获得审核组成员列表")
    @Parameter(name = "planId", description = "审核计划 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<List<QmsAuditPlanAuditorRespVO>> getAuditorList(@RequestParam("planId") Long planId) {
        List<QmsAuditPlanAuditorDO> list = auditPlanService.getAuditorListByPlanId(planId);
        return success(BeanUtils.toBean(list, QmsAuditPlanAuditorRespVO.class));
    }

}
