package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitabilityAnalysisDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpProfitabilityAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 获利能力分析")
@RestController
@RequestMapping("/erp/profitability-analysis")
@Validated
public class ErpProfitabilityAnalysisController {

    @Resource
    private ErpProfitabilityAnalysisService profitabilityAnalysisService;

    @PostMapping("/create")
    @Operation(summary = "创建获利分析记录")
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:create')")
    public CommonResult<Long> createProfitabilityAnalysis(@Valid @RequestBody ErpProfitabilityAnalysisSaveReqVO createReqVO) {
        return success(profitabilityAnalysisService.createProfitabilityAnalysis(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新获利分析记录")
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:update')")
    public CommonResult<Boolean> updateProfitabilityAnalysis(@Valid @RequestBody ErpProfitabilityAnalysisSaveReqVO updateReqVO) {
        profitabilityAnalysisService.updateProfitabilityAnalysis(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除获利分析记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:delete')")
    public CommonResult<Boolean> deleteProfitabilityAnalysis(@RequestParam("id") Long id) {
        profitabilityAnalysisService.deleteProfitabilityAnalysis(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得获利分析记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:query')")
    public CommonResult<ErpProfitabilityAnalysisRespVO> getProfitabilityAnalysis(@RequestParam("id") Long id) {
        ErpProfitabilityAnalysisDO analysis = profitabilityAnalysisService.getProfitabilityAnalysis(id);
        return success(BeanUtils.toBean(analysis, ErpProfitabilityAnalysisRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得获利分析分页")
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:query')")
    public CommonResult<PageResult<ErpProfitabilityAnalysisRespVO>> getProfitabilityAnalysisPage(
            @Valid ErpProfitabilityAnalysisPageReqVO pageReqVO) {
        PageResult<ErpProfitabilityAnalysisDO> pageResult = profitabilityAnalysisService.getProfitabilityAnalysisPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpProfitabilityAnalysisRespVO.class));
    }

    @PostMapping("/calculate")
    @Operation(summary = "计算获利能力", description = "按利润中心 + 期间计算利润和利润率（不存在则新建）")
    @Parameters({
            @Parameter(name = "profitCenterId", description = "利润中心编号", required = true),
            @Parameter(name = "periodId", description = "会计期间编号", required = true)
    })
    @PreAuthorize("@ss.hasPermission('erp:profitability-analysis:create')")
    public CommonResult<ErpProfitabilityAnalysisRespVO> calculateProfitability(
            @RequestParam("profitCenterId") Long profitCenterId,
            @RequestParam("periodId") Long periodId) {
        ErpProfitabilityAnalysisDO analysis = profitabilityAnalysisService
                .calculateProfitability(profitCenterId, periodId);
        return success(BeanUtils.toBean(analysis, ErpProfitabilityAnalysisRespVO.class));
    }

}
