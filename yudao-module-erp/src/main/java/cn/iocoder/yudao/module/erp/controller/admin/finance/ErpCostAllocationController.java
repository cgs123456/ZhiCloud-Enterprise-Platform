package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostAllocationDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpCostAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 成本分摊")
@RestController
@RequestMapping("/erp/cost-allocation")
@Validated
public class ErpCostAllocationController {

    @Resource
    private ErpCostAllocationService costAllocationService;

    @PostMapping("/create")
    @Operation(summary = "创建成本分摊记录")
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:create')")
    public CommonResult<Long> createCostAllocation(@Valid @RequestBody ErpCostAllocationSaveReqVO createReqVO) {
        return success(costAllocationService.createCostAllocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成本分摊记录")
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:update')")
    public CommonResult<Boolean> updateCostAllocation(@Valid @RequestBody ErpCostAllocationSaveReqVO updateReqVO) {
        costAllocationService.updateCostAllocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成本分摊记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:delete')")
    public CommonResult<Boolean> deleteCostAllocation(@RequestParam("id") Long id) {
        costAllocationService.deleteCostAllocation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成本分摊记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:query')")
    public CommonResult<ErpCostAllocationRespVO> getCostAllocation(@RequestParam("id") Long id) {
        ErpCostAllocationDO allocation = costAllocationService.getCostAllocation(id);
        return success(BeanUtils.toBean(allocation, ErpCostAllocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成本分摊分页")
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:query')")
    public CommonResult<PageResult<ErpCostAllocationRespVO>> getCostAllocationPage(@Valid ErpCostAllocationPageReqVO pageReqVO) {
        PageResult<ErpCostAllocationDO> pageResult = costAllocationService.getCostAllocationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCostAllocationRespVO.class));
    }

    @PostMapping("/allocate")
    @Operation(summary = "执行成本分摊", description = "从源成本中心向目标成本中心分摊指定金额（手工分摊）")
    @Parameters({
            @Parameter(name = "costCenterId", description = "源成本中心编号", required = true),
            @Parameter(name = "targetId", description = "目标成本中心编号", required = true),
            @Parameter(name = "amount", description = "分摊金额", required = true),
            @Parameter(name = "date", description = "分摊日期（默认今天）", example = "2026-07-29")
    })
    @PreAuthorize("@ss.hasPermission('erp:cost-allocation:create')")
    public CommonResult<Long> allocateCost(
            @RequestParam("costCenterId") Long costCenterId,
            @RequestParam("targetId") Long targetId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return success(costAllocationService.allocateCost(costCenterId, targetId, amount, date));
    }

}
