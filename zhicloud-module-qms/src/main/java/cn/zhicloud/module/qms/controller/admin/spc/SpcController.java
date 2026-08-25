package cn.zhicloud.module.qms.controller.admin.spc;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.qms.controller.admin.spc.vo.SamplingPlanRespVO;
import cn.zhicloud.module.qms.controller.admin.spc.vo.SpcAnalysisRespVO;
import cn.zhicloud.module.qms.service.spc.SpcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * SPC 统计过程控制 Controller
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - QMS SPC 统计过程控制")
@RestController
@RequestMapping("/qms/spc")
@Validated
public class SpcController {

    @Resource
    private SpcService spcService;

    @GetMapping("/analyze")
    @Operation(summary = "工序能力分析（Cp/Cpk/控制限）")
    @Parameter(name = "itemId", description = "检验项目 ID", required = true)
    @PreAuthorize("@ss.hasPermission('qms:spc:query')")
    public CommonResult<SpcAnalysisRespVO> analyze(@RequestParam("itemId") Long itemId) {
        return success(spcService.analyze(itemId));
    }

    @GetMapping("/sampling-plan")
    @Operation(summary = "抽样方案查询（MIL-STD-105E / GB 2828）")
    @PreAuthorize("@ss.hasPermission('qms:spc:query')")
    public CommonResult<SamplingPlanRespVO> getSamplingPlan(
            @RequestParam("lotSize") Long lotSize,
            @RequestParam(value = "inspectionLevel", required = false, defaultValue = "II") String inspectionLevel,
            @RequestParam(value = "aql", required = false) BigDecimal aql) {
        return success(spcService.getSamplingPlan(lotSize, inspectionLevel, aql));
    }

}
