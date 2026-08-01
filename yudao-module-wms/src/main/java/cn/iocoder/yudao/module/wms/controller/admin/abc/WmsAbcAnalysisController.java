package cn.iocoder.yudao.module.wms.controller.admin.abc;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcAnalysisReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcReportRespVO;
import cn.iocoder.yudao.module.wms.service.abc.WmsAbcAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * WMS ABC 分类分析 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS ABC 分类分析")
@RestController
@RequestMapping("/wms/abc-analysis")
@Validated
public class WmsAbcAnalysisController {

    @Resource
    private WmsAbcAnalysisService abcAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "触发 ABC 分类分析")
    @PreAuthorize("@ss.hasPermission('wms:abc-analysis:update')")
    public CommonResult<WmsAbcReportRespVO> analyzeAbcClassification(@Valid @RequestBody WmsAbcAnalysisReqVO reqVO) {
        return success(abcAnalysisService.analyzeAbcClassification(reqVO));
    }

    @GetMapping("/report")
    @Operation(summary = "获取 ABC 分类报告")
    @PreAuthorize("@ss.hasPermission('wms:abc-analysis:query')")
    public CommonResult<WmsAbcReportRespVO> getAbcReport() {
        return success(abcAnalysisService.getAbcReport());
    }

}
