package cn.zhicloud.module.qms.controller.admin.audit;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.audit.vo.*;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditNonconformityDO;
import cn.zhicloud.module.qms.framework.electronicsignature.ElectronicSignature;
import cn.zhicloud.module.qms.service.audit.QmsAuditNonconformityService;
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
 * QMS 审核不符合项 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - QMS 审核不符合项")
@RestController
@RequestMapping("/qms/audit-nonconformity")
@Validated
public class QmsAuditNonconformityController {

    @Resource
    private QmsAuditNonconformityService nonconformityService;

    @PostMapping("/create")
    @Operation(summary = "新增不符合项")
    @PreAuthorize("@ss.hasPermission('qms:audit:create')")
    public CommonResult<Long> createNonconformity(@Valid @RequestBody QmsAuditNonconformitySaveReqVO createReqVO) {
        return success(nonconformityService.createNonconformity(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新不符合项")
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Boolean> updateNonconformity(@Valid @RequestBody QmsAuditNonconformitySaveReqVO updateReqVO) {
        nonconformityService.updateNonconformity(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除不符合项")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:audit:delete')")
    public CommonResult<Boolean> deleteNonconformity(@RequestParam("id") Long id) {
        nonconformityService.deleteNonconformity(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得不符合项")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<QmsAuditNonconformityRespVO> getNonconformity(@RequestParam("id") Long id) {
        QmsAuditNonconformityDO nonconformity = nonconformityService.getNonconformity(id);
        return success(BeanUtils.toBean(nonconformity, QmsAuditNonconformityRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得不符合项分页")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<PageResult<QmsAuditNonconformityRespVO>> getNonconformityPage(@Valid QmsAuditNonconformityPageReqVO pageReqVO) {
        PageResult<QmsAuditNonconformityDO> pageResult = nonconformityService.getNonconformityPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsAuditNonconformityRespVO.class));
    }

    @PutMapping("/rectify")
    @Operation(summary = "提交整改措施", description = "仅在状态为「待整改」时允许，流转为「整改中」")
    @PreAuthorize("@ss.hasPermission('qms:audit:rectify')")
    public CommonResult<Boolean> rectifyNonconformity(@Valid @RequestBody QmsAuditNonconformityRectifyReqVO reqVO) {
        nonconformityService.rectifyNonconformity(reqVO);
        return success(true);
    }

    @PutMapping("/verify")
    @Operation(summary = "验证整改效果", description = "仅在状态为「已整改」时允许，流转为「已验证」")
    @PreAuthorize("@ss.hasPermission('qms:audit:verify')")
    @ElectronicSignature(meaning = "审核不符合项整改验证", requireReason = true)
    public CommonResult<Boolean> verifyNonconformity(@Valid @RequestBody QmsAuditNonconformityVerifyReqVO reqVO) {
        nonconformityService.verifyNonconformity(reqVO);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭不符合项", description = "仅在状态为「已验证」时允许，流转为「已关闭」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:close')")
    @ElectronicSignature(meaning = "审核不符合项关闭", requireReason = true)
    public CommonResult<Boolean> closeNonconformity(@RequestParam("id") Long id) {
        nonconformityService.closeNonconformity(id);
        return success(true);
    }

    @GetMapping("/list-by-report")
    @Operation(summary = "获得审核报告关联的不符合项列表")
    @Parameter(name = "reportId", description = "审核报告 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<List<QmsAuditNonconformityRespVO>> getNonconformityListByReportId(@RequestParam("reportId") Long reportId) {
        List<QmsAuditNonconformityDO> list = nonconformityService.getNonconformityListByReportId(reportId);
        return success(BeanUtils.toBean(list, QmsAuditNonconformityRespVO.class));
    }

}
