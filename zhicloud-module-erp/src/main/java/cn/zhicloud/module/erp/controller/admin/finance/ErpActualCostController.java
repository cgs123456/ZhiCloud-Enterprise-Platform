package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import cn.zhicloud.module.erp.service.finance.cost.ErpActualCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 实际成本")
@RestController
@RequestMapping("/erp/actual-cost")
@Validated
public class ErpActualCostController {

    @Resource
    private ErpActualCostService actualCostService;

    @PostMapping("/create")
    @Operation(summary = "创建实际成本")
    @PreAuthorize("@ss.hasPermission('erp:actual-cost:create')")
    public CommonResult<Long> createActualCost(@Valid @RequestBody ErpActualCostSaveReqVO createReqVO) {
        return success(actualCostService.createActualCost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新实际成本")
    @PreAuthorize("@ss.hasPermission('erp:actual-cost:update')")
    public CommonResult<Boolean> updateActualCost(@Valid @RequestBody ErpActualCostSaveReqVO updateReqVO) {
        actualCostService.updateActualCost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除实际成本")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:actual-cost:delete')")
    public CommonResult<Boolean> deleteActualCost(@RequestParam("id") Long id) {
        actualCostService.deleteActualCost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得实际成本")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:actual-cost:query')")
    public CommonResult<ErpActualCostRespVO> getActualCost(@RequestParam("id") Long id) {
        ErpActualCostDO actualCost = actualCostService.getActualCost(id);
        return success(BeanUtils.toBean(actualCost, ErpActualCostRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得实际成本分页")
    @PreAuthorize("@ss.hasPermission('erp:actual-cost:query')")
    public CommonResult<PageResult<ErpActualCostRespVO>> getActualCostPage(@Valid ErpActualCostPageReqVO pageReqVO) {
        PageResult<ErpActualCostDO> pageResult = actualCostService.getActualCostPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpActualCostRespVO.class));
    }

}
