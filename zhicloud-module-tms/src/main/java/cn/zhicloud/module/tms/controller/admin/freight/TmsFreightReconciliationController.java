package cn.zhicloud.module.tms.controller.admin.freight;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationRespVO;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import cn.zhicloud.module.tms.service.freight.TmsFreightReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - TMS 运费对账
 *
 * @author 智云
 */
@Tag(name = "管理后台 - TMS 运费对账")
@RestController
@RequestMapping("/tms/freight-reconciliation")
@PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:query')")
public class TmsFreightReconciliationController {

    @Resource
    private TmsFreightReconciliationService reconciliationService;

    @PostMapping("/create")
    @Operation(summary = "创建运费对账单")
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:create')")
    public CommonResult<Long> createReconciliation(@Valid @RequestBody TmsFreightReconciliationSaveReqVO createReqVO) {
        return success(reconciliationService.createReconciliation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新运费对账单")
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:update')")
    public CommonResult<Boolean> updateReconciliation(@Valid @RequestBody TmsFreightReconciliationSaveReqVO updateReqVO) {
        reconciliationService.updateReconciliation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除运费对账单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:delete')")
    public CommonResult<Boolean> deleteReconciliation(@RequestParam("id") Long id) {
        reconciliationService.deleteReconciliation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取运费对账单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<TmsFreightReconciliationRespVO> getReconciliation(@RequestParam("id") Long id) {
        TmsFreightReconciliationDO reconciliation = reconciliationService.getReconciliation(id);
        return success(BeanUtils.toBean(reconciliation, TmsFreightReconciliationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取运费对账分页")
    public CommonResult<PageResult<TmsFreightReconciliationRespVO>> getReconciliationPage(@Valid TmsFreightReconciliationPageReqVO pageReqVO) {
        PageResult<TmsFreightReconciliationDO> pageResult = reconciliationService.getReconciliationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsFreightReconciliationRespVO.class));
    }

    @PostMapping("/do-reconcile")
    @Operation(summary = "执行对账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:update')")
    public CommonResult<Boolean> doReconcile(@RequestParam("id") Long id) {
        reconciliationService.doReconcile(id);
        return success(true);
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认对账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:update')")
    public CommonResult<Boolean> confirmReconciliation(@RequestParam("id") Long id) {
        reconciliationService.confirmReconciliation(id);
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回对账")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "reason", description = "驳回原因")
    @PreAuthorize("@ss.hasPermission('tms:freight-reconciliation:update')")
    public CommonResult<Boolean> rejectReconciliation(@RequestParam("id") Long id,
                                                       @RequestParam(value = "reason", required = false) String reason) {
        reconciliationService.rejectReconciliation(id, reason);
        return success(true);
    }

}
