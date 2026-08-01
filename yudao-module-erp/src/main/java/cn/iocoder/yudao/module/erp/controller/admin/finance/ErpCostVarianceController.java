package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVariancePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVarianceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVarianceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import cn.iocoder.yudao.module.erp.service.finance.cost.ErpCostVarianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 成本差异")
@RestController
@RequestMapping("/erp/cost-variance")
@Validated
public class ErpCostVarianceController {

    @Resource
    private ErpCostVarianceService costVarianceService;

    @PostMapping("/create")
    @Operation(summary = "创建成本差异")
    @PreAuthorize("@ss.hasPermission('erp:cost-variance:create')")
    public CommonResult<Long> createCostVariance(@Valid @RequestBody ErpCostVarianceSaveReqVO createReqVO) {
        return success(costVarianceService.createCostVariance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成本差异")
    @PreAuthorize("@ss.hasPermission('erp:cost-variance:update')")
    public CommonResult<Boolean> updateCostVariance(@Valid @RequestBody ErpCostVarianceSaveReqVO updateReqVO) {
        costVarianceService.updateCostVariance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成本差异")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cost-variance:delete')")
    public CommonResult<Boolean> deleteCostVariance(@RequestParam("id") Long id) {
        costVarianceService.deleteCostVariance(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成本差异")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cost-variance:query')")
    public CommonResult<ErpCostVarianceRespVO> getCostVariance(@RequestParam("id") Long id) {
        ErpCostVarianceDO costVariance = costVarianceService.getCostVariance(id);
        return success(BeanUtils.toBean(costVariance, ErpCostVarianceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成本差异分页")
    @PreAuthorize("@ss.hasPermission('erp:cost-variance:query')")
    public CommonResult<PageResult<ErpCostVarianceRespVO>> getCostVariancePage(@Valid ErpCostVariancePageReqVO pageReqVO) {
        PageResult<ErpCostVarianceDO> pageResult = costVarianceService.getCostVariancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCostVarianceRespVO.class));
    }

}
