package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import cn.iocoder.yudao.module.erp.service.finance.cost.ErpWorkOrderCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 工单成本归集")
@RestController
@RequestMapping("/erp/work-order-cost")
@Validated
public class ErpWorkOrderCostController {

    @Resource
    private ErpWorkOrderCostService workOrderCostService;

    @PostMapping("/create")
    @Operation(summary = "创建工单成本归集")
    @PreAuthorize("@ss.hasPermission('erp:work-order-cost:create')")
    public CommonResult<Long> createWorkOrderCost(@Valid @RequestBody ErpWorkOrderCostSaveReqVO createReqVO) {
        return success(workOrderCostService.createWorkOrderCost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单成本归集")
    @PreAuthorize("@ss.hasPermission('erp:work-order-cost:update')")
    public CommonResult<Boolean> updateWorkOrderCost(@Valid @RequestBody ErpWorkOrderCostSaveReqVO updateReqVO) {
        workOrderCostService.updateWorkOrderCost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单成本归集")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:work-order-cost:delete')")
    public CommonResult<Boolean> deleteWorkOrderCost(@RequestParam("id") Long id) {
        workOrderCostService.deleteWorkOrderCost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单成本归集")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:work-order-cost:query')")
    public CommonResult<ErpWorkOrderCostRespVO> getWorkOrderCost(@RequestParam("id") Long id) {
        ErpWorkOrderCostDO workOrderCost = workOrderCostService.getWorkOrderCost(id);
        return success(BeanUtils.toBean(workOrderCost, ErpWorkOrderCostRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单成本归集分页")
    @PreAuthorize("@ss.hasPermission('erp:work-order-cost:query')")
    public CommonResult<PageResult<ErpWorkOrderCostRespVO>> getWorkOrderCostPage(@Valid ErpWorkOrderCostPageReqVO pageReqVO) {
        PageResult<ErpWorkOrderCostDO> pageResult = workOrderCostService.getWorkOrderCostPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpWorkOrderCostRespVO.class));
    }

}
