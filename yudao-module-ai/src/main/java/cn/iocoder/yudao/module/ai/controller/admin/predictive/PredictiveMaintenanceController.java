package cn.iocoder.yudao.module.ai.controller.admin.predictive;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ai.controller.admin.predictive.vo.PredictiveMaintenanceReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.predictive.vo.PredictiveMaintenanceRespVO;
import cn.iocoder.yudao.module.ai.service.predictive.FailurePredictionResult;
import cn.iocoder.yudao.module.ai.service.predictive.PredictiveMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * AI 预测性维护 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - AI 预测性维护")
@RestController
@RequestMapping("/ai/predictive/maintenance")
public class PredictiveMaintenanceController {

    @Resource
    private PredictiveMaintenanceService predictiveMaintenanceService;

    @PostMapping("/predict")
    @Operation(summary = "预测设备故障概率")
    @PreAuthorize("@ss.hasPermission('ai:predictive:maintenance:predict')")
    public CommonResult<PredictiveMaintenanceRespVO> predict(@Valid @RequestBody PredictiveMaintenanceReqVO reqVO) {
        FailurePredictionResult result = predictiveMaintenanceService.predictFailure(reqVO.getDeviceId());
        return success(toRespVO(result));
    }

    @PostMapping("/recommendation")
    @Operation(summary = "获取维护建议")
    @PreAuthorize("@ss.hasPermission('ai:predictive:maintenance:recommend')")
    public CommonResult<PredictiveMaintenanceRespVO> recommendation(@Valid @RequestBody PredictiveMaintenanceReqVO reqVO) {
        FailurePredictionResult result = predictiveMaintenanceService.predictFailure(reqVO.getDeviceId());
        String recommendation = predictiveMaintenanceService.getMaintenanceRecommendation(reqVO.getDeviceId());
        PredictiveMaintenanceRespVO respVO = toRespVO(result);
        respVO.setRecommendation(recommendation);
        return success(respVO);
    }

    private PredictiveMaintenanceRespVO toRespVO(FailurePredictionResult result) {
        PredictiveMaintenanceRespVO respVO = new PredictiveMaintenanceRespVO();
        respVO.setDeviceId(result.getDeviceId());
        respVO.setProbability(result.getProbability());
        respVO.setRiskLevel(result.getRiskLevel() != null ? result.getRiskLevel().name() : null);
        respVO.setRecommendedDate(result.getRecommendedDate());
        respVO.setReasoning(result.getReasoning());
        return respVO;
    }

}
