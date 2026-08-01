package cn.iocoder.yudao.module.qms.controller.admin.sqm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import cn.iocoder.yudao.module.qms.service.sqm.SupplierAuditService;
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
 * QMS 供应商审核 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 供应商审核")
@RestController
@RequestMapping("/qms/supplier-audit")
@Validated
public class SupplierAuditController {

    @Resource
    private SupplierAuditService supplierAuditService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商审核")
    @PreAuthorize("@ss.hasPermission('qms:supplier-audit:create')")
    public CommonResult<Long> createSupplierAudit(@Valid @RequestBody SupplierAuditSaveReqVO createReqVO) {
        return success(supplierAuditService.createSupplierAudit(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商审核")
    @PreAuthorize("@ss.hasPermission('qms:supplier-audit:update')")
    public CommonResult<Boolean> updateSupplierAudit(@Valid @RequestBody SupplierAuditSaveReqVO updateReqVO) {
        supplierAuditService.updateSupplierAudit(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商审核")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:supplier-audit:delete')")
    public CommonResult<Boolean> deleteSupplierAudit(@RequestParam("id") Long id) {
        supplierAuditService.deleteSupplierAudit(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商审核")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:supplier-audit:query')")
    public CommonResult<SupplierAuditRespVO> getSupplierAudit(@RequestParam("id") Long id) {
        SupplierAuditDO supplierAudit = supplierAuditService.getSupplierAudit(id);
        return success(BeanUtils.toBean(supplierAudit, SupplierAuditRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商审核分页")
    @PreAuthorize("@ss.hasPermission('qms:supplier-audit:query')")
    public CommonResult<PageResult<SupplierAuditRespVO>> getSupplierAuditPage(@Valid SupplierAuditPageReqVO pageReqVO) {
        PageResult<SupplierAuditDO> pageResult = supplierAuditService.getSupplierAuditPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SupplierAuditRespVO.class));
    }

}