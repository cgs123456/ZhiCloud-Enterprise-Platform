package cn.iocoder.yudao.module.qms.controller.admin.traceability;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.qms.controller.admin.traceability.vo.QualityTraceabilityRespVO;
import cn.iocoder.yudao.module.qms.service.traceability.QualityTraceabilityService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 质量追溯 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 质量追溯")
@RestController
@RequestMapping("/qms/traceability")
@Validated
public class QualityTraceabilityController {

    @Resource
    private QualityTraceabilityService qualityTraceabilityService;

    @GetMapping("/forward")
    @Operation(summary = "正向追溯", description = "原料 → 成品：查询原料批次关联的生产工单及工单产出的成品批次")
    @PreAuthorize("@ss.hasPermission('qms:traceability:query')")
    public CommonResult<QualityTraceabilityRespVO> forwardTrace(
            @Parameter(description = "物料 ID") @RequestParam(value = "materialId", required = false) Long materialId,
            @Parameter(description = "批次号", required = true) @RequestParam("batchNo") String batchNo) {
        return success(qualityTraceabilityService.forwardTrace(materialId, batchNo));
    }

    @GetMapping("/backward")
    @Operation(summary = "反向追溯", description = "成品 → 原料：查询成品批次关联的生产工单及工单领用的原料批次")
    @PreAuthorize("@ss.hasPermission('qms:traceability:query')")
    public CommonResult<QualityTraceabilityRespVO> backwardTrace(
            @Parameter(description = "产品 ID") @RequestParam(value = "productId", required = false) Long productId,
            @Parameter(description = "批次号", required = true) @RequestParam("batchNo") String batchNo) {
        return success(qualityTraceabilityService.backwardTrace(productId, batchNo));
    }

}
