package cn.zhicloud.module.airag.service.eval;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.airag.config.AiragEvalAsyncConfiguration;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalDatasetDO;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalQuestionDO;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalReportDO;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalDatasetMapper;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalQuestionMapper;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalReportMapper;
import cn.zhicloud.module.airag.evaluation.RagEvaluationRequest;
import cn.zhicloud.module.airag.evaluation.RagEvaluationResult;
import cn.zhicloud.module.airag.evaluation.RagEvaluationService;
import cn.zhicloud.module.airag.service.knowledge.AiragKnowledgeService;
import cn.zhicloud.module.airag.service.rag.AiragRagService;
import cn.zhicloud.module.airag.service.rag.Bm25LexicalRetriever;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.*;

/**
 * RAG 批量评估 Service
 *
 * <p>流程：数据集（问题集）→ 逐题 RAG 作答 → 4 指标打分 → 聚合报告 → 与上次对比。
 * 批量执行为耗时操作（每题 2 次 LLM 调用），必须走异步；进度通过报告行的 done_count 轮询。
 *
 * <p>上下文来源说明：评估用上下文取自 BM25 词法路径（与主链路同一分词器，保证一致性），
 * topK 取 8。趋势对比仅在相同 topK 下有意义，更改 topK 后历史曲线仅供参考。
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class RagEvalBatchService {

    /**
     * 评估用上下文召回数（与主链路 FUSE_TOP_K=8 对齐）
     */
    private static final int EVAL_CONTEXT_TOP_K = 8;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 报告状态：执行中
     */
    public static final int REPORT_STATUS_RUNNING = 0;
    /**
     * 报告状态：已完成
     */
    public static final int REPORT_STATUS_DONE = 1;
    /**
     * 报告状态：失败
     */
    public static final int REPORT_STATUS_FAILED = 2;

    @Resource
    private RagEvalBatchService self; // 自注入：走代理调用，同类内 @Async 方法自调用会被绕过代理导致同步执行

    @Resource
    private AiragEvalDatasetMapper datasetMapper;
    @Resource
    private AiragEvalQuestionMapper questionMapper;
    @Resource
    private AiragEvalReportMapper reportMapper;
    @Resource
    private AiragKnowledgeService knowledgeService;
    @Resource
    private AiragRagService ragService;
    @Resource
    private RagEvaluationService evaluationService;
    /**
     * 可选 BM25 词法检索器（向量底表未启用时为 null，此时评估降级为仅答案相关性）
     */
    @Autowired(required = false)
    private Bm25LexicalRetriever bm25Retriever;

    // ==================== 数据集管理 ====================

    /**
     * 创建评估数据集
     */
    public Long createDataset(String name, String description, Long knowledgeId) {
        knowledgeService.validateKnowledgeExists(knowledgeId);
        AiragEvalDatasetDO dataset = new AiragEvalDatasetDO()
                .setName(name)
                .setDescription(description)
                .setKnowledgeId(knowledgeId)
                .setQuestionCount(0)
                .setStatus(0);
        datasetMapper.insert(dataset);
        return dataset.getId();
    }

    /**
     * 批量导入问题（追加模式，导入后刷新题目数冗余）
     *
     * <p>纯本地 DB 写，无外部调用，加事务保证题目与计数一致。
     *
     * @return 导入条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int importQuestions(Long datasetId, List<AiragEvalQuestionDO> questions) {
        AiragEvalDatasetDO dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw exception(EVAL_DATASET_NOT_EXISTS);
        }
        int sort = 0;
        for (AiragEvalQuestionDO q : questions) {
            q.setId(null);
            q.setDatasetId(datasetId);
            if (q.getSort() == null) {
                q.setSort(sort);
            }
            sort++;
            questionMapper.insert(q);
        }
        Long count = questionMapper.selectCountByDatasetId(datasetId);
        datasetMapper.updateById(new AiragEvalDatasetDO()
                .setId(datasetId)
                .setQuestionCount(count.intValue()));
        return questions.size();
    }

    /**
     * 查询评估数据集列表
     */
    public List<AiragEvalDatasetDO> getDatasetList(Long knowledgeId) {
        return datasetMapper.selectListByKnowledgeId(knowledgeId);
    }

    /**
     * 查询数据集最新一次已完成的报告
     */
    public AiragEvalReportDO getLatestDoneReport(Long datasetId) {
        return reportMapper.selectLatestDoneByDatasetId(datasetId);
    }

    /**
     * 查询报告历史趋势（按创建时间倒序，默认最近 20 次）
     */
    public List<AiragEvalReportDO> getReportTrend(Long datasetId, Integer limit) {
        int size = limit == null || limit <= 0 || limit > 100 ? 20 : limit;
        return reportMapper.selectListByDatasetIdDesc(datasetId, size);
    }

    // ==================== 批量执行 ====================

    /**
     * 触发异步批量评估（立即返回报告编号，前端轮询进度）
     *
     * @return 评估报告编号（初始状态为执行中）
     */
    public Long runBatchAsync(Long datasetId, Long tenantId) {
        AiragEvalDatasetDO dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw exception(EVAL_DATASET_NOT_EXISTS);
        }
        List<AiragEvalQuestionDO> questions = questionMapper.selectListByDatasetId(datasetId);
        if (CollUtil.isEmpty(questions)) {
            throw exception(EVAL_DATASET_EMPTY);
        }
        // 同一数据集同时只允许一个执行中的批量任务
        List<AiragEvalReportDO> history = reportMapper.selectListByDatasetIdDesc(datasetId, 5);
        boolean running = history.stream().anyMatch(r -> Integer.valueOf(REPORT_STATUS_RUNNING).equals(r.getStatus()));
        if (running) {
            throw exception(EVAL_BATCH_ALREADY_RUNNING);
        }
        AiragEvalReportDO report = new AiragEvalReportDO()
                .setDatasetId(datasetId)
                .setKnowledgeId(dataset.getKnowledgeId())
                .setStatus(REPORT_STATUS_RUNNING)
                .setTotalCount(questions.size())
                .setDoneCount(0);
        reportMapper.insert(report);
        self.executeBatchAsync(report.getId(), datasetId, tenantId);
        return report.getId();
    }

    /**
     * 异步执行体（虚拟线程池，租户上下文显式透传，@Async 代理不继承 ThreadLocal）
     */
    @Async(AiragEvalAsyncConfiguration.EVAL_EXECUTOR_BEAN_NAME)
    public void executeBatchAsync(Long reportId, Long datasetId, Long tenantId) {
        TenantUtils.execute(tenantId, () -> runBatch(reportId, datasetId, tenantId));
    }

    /**
     * 同步执行批量评估（逐题作答 + 打分 + 聚合落库）
     */
    // @tx-ignore 单题独立、可部分成功，不要求整体原子；进度靠 done_count 逐题落库，失败题跳过
    public void runBatch(Long reportId, Long datasetId, Long tenantId) {
        AiragEvalReportDO report = reportMapper.selectById(reportId);
        if (report == null) {
            throw exception(EVAL_REPORT_NOT_EXISTS);
        }
        List<AiragEvalQuestionDO> questions = questionMapper.selectListByDatasetId(datasetId);
        List<Map<String, Object>> singleResults = new ArrayList<>();
        List<RagEvalReportAggregator.SingleResult> singles = new ArrayList<>();
        int failed = 0;
        for (AiragEvalQuestionDO q : questions) {
            try {
                // 1. RAG 作答
                String answer = ragService.chat(report.getKnowledgeId(), q.getQuestion());
                // 2. BM25 路径取评估上下文（与主链路同一分词器）
                List<String> contexts = retrieveContexts(q.getQuestion(), report.getKnowledgeId(), tenantId);
                // 3. 4 指标打分
                RagEvaluationRequest req = new RagEvaluationRequest();
                req.setQuestion(q.getQuestion());
                req.setAnswer(answer);
                req.setContexts(contexts);
                req.setGroundTruth(q.getGroundTruth());
                RagEvaluationResult r = evaluationService.evaluate(req);
                singles.add(new RagEvalReportAggregator.SingleResult(q.getId(),
                        r.getFaithfulness(), r.getAnswerRelevancy(),
                        r.getContextPrecision(), r.getContextRecall()));
                Map<String, Object> one = new HashMap<>();
                one.put("questionId", q.getId());
                one.put("question", q.getQuestion());
                one.put("answer", answer);
                one.put("faithfulness", r.getFaithfulness());
                one.put("answerRelevancy", r.getAnswerRelevancy());
                one.put("contextPrecision", r.getContextPrecision());
                one.put("contextRecall", r.getContextRecall());
                one.put("overall", r.getOverallScore());
                singleResults.add(one);
            } catch (Exception e) {
                failed++;
                log.warn("[runBatch][题目评估失败，reportId={}, questionId={}]", reportId, q.getId(), e);
            } finally {
                reportMapper.updateById(new AiragEvalReportDO()
                        .setId(reportId)
                        .setDoneCount(singleResults.size() + failed));
            }
        }
        if (singles.isEmpty()) {
            reportMapper.updateById(new AiragEvalReportDO()
                    .setId(reportId)
                    .setStatus(REPORT_STATUS_FAILED)
                    .setErrorMsg("全部题目评估失败，请检查知识库与 LLM 配置"));
            return;
        }
        // 4. 与上次已完成报告对比
        AiragEvalReportDO previous = reportMapper.selectLatestDoneByDatasetId(datasetId);
        Map<Long, Double> prevMap = parsePreviousOverall(previous);
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(singles, prevMap);
        // 5. 落库
        AiragEvalReportDO update = new AiragEvalReportDO()
                .setId(reportId)
                .setStatus(REPORT_STATUS_DONE)
                .setAvgFaithfulness(agg.avgFaithfulness())
                .setAvgAnswerRelevancy(agg.avgAnswerRelevancy())
                .setAvgContextPrecision(agg.avgContextPrecision())
                .setAvgContextRecall(agg.avgContextRecall())
                .setOverallScore(agg.overallScore());
        try {
            update.setResultsJson(OBJECT_MAPPER.writeValueAsString(singleResults));
            update.setRegressedQuestions(OBJECT_MAPPER.writeValueAsString(agg.regressedQuestionIds()));
            update.setImprovedQuestions(OBJECT_MAPPER.writeValueAsString(agg.improvedQuestionIds()));
        } catch (Exception e) {
            log.warn("[runBatch][报告 JSON 序列化失败，reportId={}]", reportId, e);
        }
        if (previous != null && previous.getOverallScore() != null && agg.overallScore() != null) {
            update.setOverallDelta(agg.overallScore() - previous.getOverallScore());
        }
        reportMapper.updateById(update);
        log.info("[runBatch][批量评估完成，reportId={}, overall={}, failed={}]",
                reportId, agg.overallScore(), failed);
    }

    /**
     * 取评估上下文（BM25 词法路径；底表未启用时返回空列表，评估降级）
     */
    private List<String> retrieveContexts(String question, Long knowledgeId, Long tenantId) {
        if (bm25Retriever == null) {
            return java.util.Collections.emptyList();
        }
        try {
            return bm25Retriever.retrieve(question, knowledgeId, tenantId, EVAL_CONTEXT_TOP_K)
                    .stream()
                    .map(Document::getText)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[retrieveContexts][BM25 上下文获取失败，降级为空]", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 解析上次报告的单题综合分（损坏则放弃对比，不阻断本次落库）
     */
    private Map<Long, Double> parsePreviousOverall(AiragEvalReportDO previous) {
        Map<Long, Double> map = new HashMap<>();
        if (previous == null || previous.getResultsJson() == null) {
            return map;
        }
        try {
            List<Map<String, Object>> list = OBJECT_MAPPER.readValue(previous.getResultsJson(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> one : list) {
                Object qid = one.get("questionId");
                Object overall = one.get("overall");
                if (qid instanceof Number && overall instanceof Number) {
                    map.put(((Number) qid).longValue(), ((Number) overall).doubleValue());
                }
            }
        } catch (Exception e) {
            log.warn("[parsePreviousOverall][上次报告解析失败，跳过对比]", e);
        }
        return map;
    }

}
