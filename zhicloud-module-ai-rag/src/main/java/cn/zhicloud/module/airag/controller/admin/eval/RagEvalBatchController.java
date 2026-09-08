package cn.zhicloud.module.airag.controller.admin.eval;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.airag.controller.admin.eval.vo.RagEvalDatasetSaveReqVO;
import cn.zhicloud.module.airag.controller.admin.eval.vo.RagEvalQuestionImportReqVO;
import cn.zhicloud.module.airag.controller.admin.eval.vo.RagEvalReportRespVO;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalDatasetDO;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalQuestionDO;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalReportDO;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalDatasetMapper;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalReportMapper;
import cn.zhicloud.module.airag.service.eval.RagEvalBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * AI RAG 批量评估 Controller（数据集 + 批量执行 + 报告趋势）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - AI RAG 批量评估")
@RestController
@RequestMapping("/ai/rag/eval")
@Validated
public class RagEvalBatchController {

    @Resource
    private RagEvalBatchService batchService;
    @Resource
    private AiragEvalDatasetMapper datasetMapper;
    @Resource
    private AiragEvalReportMapper reportMapper;

    @PostMapping("/dataset/create")
    @Operation(summary = "创建评估数据集")
    @PreAuthorize("@ss.hasPermission('airag:eval:manage')")
    public CommonResult<Long> createDataset(@Valid @RequestBody RagEvalDatasetSaveReqVO reqVO) {
        return success(batchService.createDataset(reqVO.getName(), reqVO.getDescription(), reqVO.getKnowledgeId()));
    }

    @PostMapping("/dataset/import")
    @Operation(summary = "批量导入评估问题（追加）")
    @PreAuthorize("@ss.hasPermission('airag:eval:manage')")
    public CommonResult<Integer> importQuestions(@Valid @RequestBody RagEvalQuestionImportReqVO reqVO) {
        List<AiragEvalQuestionDO> questions = reqVO.getQuestions().stream().map(item -> {
            AiragEvalQuestionDO q = new AiragEvalQuestionDO();
            q.setQuestion(item.getQuestion());
            q.setGroundTruth(item.getGroundTruth());
            q.setExpectedKeywords(item.getExpectedKeywords());
            q.setDifficulty(item.getDifficulty());
            return q;
        }).collect(Collectors.toList());
        return success(batchService.importQuestions(reqVO.getDatasetId(), questions));
    }

    @GetMapping("/dataset/list")
    @Operation(summary = "查询评估数据集列表")
    @PreAuthorize("@ss.hasPermission('airag:eval:query')")
    public CommonResult<List<AiragEvalDatasetDO>> getDatasetList(
            @RequestParam(value = "knowledgeId", required = false) Long knowledgeId) {
        return success(batchService.getDatasetList(knowledgeId));
    }

    @PostMapping("/run")
    @Operation(summary = "触发批量评估（异步，返回报告编号后轮询进度）")
    @Parameter(name = "datasetId", description = "数据集编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('airag:eval:manage')")
    public CommonResult<Long> runBatch(@RequestParam("datasetId") Long datasetId) {
        // 租户 ID 一律取自服务端登录上下文
        return success(batchService.runBatchAsync(datasetId,
                cn.zhicloud.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()));
    }

    @GetMapping("/report/get")
    @Operation(summary = "查询评估报告详情（含进度）")
    @PreAuthorize("@ss.hasPermission('airag:eval:query')")
    public CommonResult<RagEvalReportRespVO> getReport(@RequestParam("id") Long id) {
        AiragEvalReportDO report = reportMapper.selectById(id);
        return success(BeanUtils.toBean(report, RagEvalReportRespVO.class));
    }

    @GetMapping("/report/latest")
    @Operation(summary = "查询数据集最新一次已完成的评估报告")
    @PreAuthorize("@ss.hasPermission('airag:eval:query')")
    public CommonResult<RagEvalReportRespVO> getLatestReport(@RequestParam("datasetId") Long datasetId) {
        AiragEvalReportDO report = batchService.getLatestDoneReport(datasetId);
        return success(BeanUtils.toBean(report, RagEvalReportRespVO.class));
    }

    @GetMapping("/report/trend")
    @Operation(summary = "查询评估报告历史趋势（按创建时间倒序）")
    @PreAuthorize("@ss.hasPermission('airag:eval:query')")
    public CommonResult<List<RagEvalReportRespVO>> getReportTrend(
            @RequestParam("datasetId") Long datasetId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        List<AiragEvalReportDO> list = batchService.getReportTrend(datasetId, limit);
        return success(BeanUtils.toBean(list, RagEvalReportRespVO.class));
    }

}
