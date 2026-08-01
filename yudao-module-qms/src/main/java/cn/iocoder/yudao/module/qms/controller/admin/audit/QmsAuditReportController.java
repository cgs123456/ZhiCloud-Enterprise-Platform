package cn.iocoder.yudao.module.qms.controller.admin.audit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import cn.iocoder.yudao.module.qms.service.audit.QmsAuditReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 审核报告 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 审核报告")
@RestController
@RequestMapping("/qms/audit-report")
@Validated
public class QmsAuditReportController {

    @Resource
    private QmsAuditReportService auditReportService;

    @PostMapping("/create")
    @Operation(summary = "创建审核报告", description = "含不符合项汇总")
    @PreAuthorize("@ss.hasPermission('qms:audit:create')")
    public CommonResult<Long> createAuditReport(@Valid @RequestBody QmsAuditReportSaveReqVO createReqVO) {
        return success(auditReportService.createAuditReport(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新审核报告")
    @PreAuthorize("@ss.hasPermission('qms:audit:update')")
    public CommonResult<Boolean> updateAuditReport(@Valid @RequestBody QmsAuditReportSaveReqVO updateReqVO) {
        auditReportService.updateAuditReport(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除审核报告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:audit:delete')")
    public CommonResult<Boolean> deleteAuditReport(@RequestParam("id") Long id) {
        auditReportService.deleteAuditReport(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得审核报告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<QmsAuditReportRespVO> getAuditReport(@RequestParam("id") Long id) {
        QmsAuditReportDO auditReport = auditReportService.getAuditReport(id);
        return success(BeanUtils.toBean(auditReport, QmsAuditReportRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得审核报告分页")
    @PreAuthorize("@ss.hasPermission('qms:audit:query')")
    public CommonResult<PageResult<QmsAuditReportRespVO>> getAuditReportPage(@Valid QmsAuditReportPageReqVO pageReqVO) {
        PageResult<QmsAuditReportDO> pageResult = auditReportService.getAuditReportPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsAuditReportRespVO.class));
    }

}
