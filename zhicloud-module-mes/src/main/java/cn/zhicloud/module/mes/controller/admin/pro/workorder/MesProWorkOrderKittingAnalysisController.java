package cn.zhicloud.module.mes.controller.admin.pro.workorder;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingSummaryRespVO;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderKittingAnalysisService;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * MES 工单齐套分析 Controller（P0-5）
 *
 * <p>对生产工单进行齐套分析：基于工单 BOM 展开物料需求，结合当前库存 + 在途数量，
 * 计算每个 BOM 行的齐套状态（齐套 / 部分齐套 / 短缺），输出整体齐套率与缺口明细。
 *
 * <h3>典型用法</h3>
 * <pre>
 * GET /mes/pro/work-order-kitting/analyze?workOrderId=123
 * </pre>
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - MES 工单齐套分析")
@RestController
@RequestMapping("/mes/pro/work-order-kitting")
@Validated
public class MesProWorkOrderKittingAnalysisController {

    @Resource
    private MesProWorkOrderKittingAnalysisService kittingAnalysisService;

    @GetMapping("/analyze")
    @Operation(summary = "工单齐套分析", description = "基于工单 BOM + 当前库存 + 在途采购入库单，计算每个 BOM 行的齐套状态")
    @Parameter(name = "workOrderId", description = "工单编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:work-order-kitting:analyze')")
    public CommonResult<MesProWorkOrderKittingSummaryRespVO> analyzeKitting(
            @RequestParam("workOrderId") Long workOrderId) {
        return success(kittingAnalysisService.analyzeKitting(workOrderId));
    }

}
