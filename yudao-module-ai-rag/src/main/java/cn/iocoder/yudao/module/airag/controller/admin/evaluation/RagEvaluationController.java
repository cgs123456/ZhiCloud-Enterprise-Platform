package cn.iocoder.yudao.module.airag.controller.admin.evaluation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.airag.controller.admin.evaluation.vo.RagEvaluationRespVO;
import cn.iocoder.yudao.module.airag.evaluation.RagEvaluationRequest;
import cn.iocoder.yudao.module.airag.evaluation.RagEvaluationResult;
import cn.iocoder.yudao.module.airag.evaluation.RagEvaluationService;
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
 * AI RAG 评估 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - AI RAG 评估")
@RestController
@RequestMapping("/ai/rag")
public class RagEvaluationController {

    @Resource
    private RagEvaluationService ragEvaluationService;

    @PostMapping("/evaluate")
    @Operation(summary = "评估 RAG 检索质量（忠实度/回答相关性/上下文精确率/上下文召回率）")
    @PreAuthorize("@ss.hasPermission('airag:evaluation:evaluate')")
    public CommonResult<RagEvaluationRespVO> evaluate(@Valid @RequestBody RagEvaluationRequest request) {
        RagEvaluationResult result = ragEvaluationService.evaluate(request);
        RagEvaluationRespVO respVO = new RagEvaluationRespVO();
        respVO.setQuestion(request.getQuestion());
        respVO.setAnswerSummary(StrUtil.sub(request.getAnswer(), 0, 200));
        respVO.setFaithfulness(result.getFaithfulness());
        respVO.setAnswerRelevancy(result.getAnswerRelevancy());
        respVO.setContextPrecision(result.getContextPrecision());
        respVO.setContextRecall(result.getContextRecall());
        respVO.setOverallScore(result.getOverallScore());
        respVO.setDetail(result.getDetail());
        return success(respVO);
    }

}
