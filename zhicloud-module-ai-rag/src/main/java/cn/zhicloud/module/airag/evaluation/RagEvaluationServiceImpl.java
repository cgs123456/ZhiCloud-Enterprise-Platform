package cn.zhicloud.module.airag.evaluation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.ai.dal.dataobject.model.AiModelDO;
import cn.zhicloud.module.ai.enums.model.AiModelTypeEnum;
import cn.zhicloud.module.ai.service.model.AiModelService;
import cn.zhicloud.module.airag.dal.dataobject.evaluation.AiRagEvaluationLogDO;
import cn.zhicloud.module.airag.dal.mysql.evaluation.AiRagEvaluationLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.RAG_EVALUATION_CONTEXTS_EMPTY;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.RAG_EVALUATION_GROUND_TRUTH_EMPTY;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.RAG_EVALUATION_LLM_UNAVAILABLE;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.RAG_EVALUATION_SCORE_PARSE_FAIL;

/**
 * RAG 评估 Service 实现类
 *
 * <h3>实现说明</h3>
 * <p>4 项指标均通过 ChatClient 调用 LLM 打分（0-1 浮点），每个指标使用独立的 prompt：
 * <ul>
 *   <li>Faithfulness：判断回答是否完全基于检索上下文，有无幻觉</li>
 *   <li>Answer Relevancy：判断回答是否切题，是否完整覆盖问题意图</li>
 *   <li>Context Precision：逐条评估检索文档与问题的相关性</li>
 *   <li>Context Recall：对比上下文与标准答案，判断是否召回所有关键信息</li>
 * </ul>
 *
 * <p>LLM 不可用时抛出 {@code RAG_EVALUATION_LLM_UNAVAILABLE}；评分解析失败时抛出
 * {@code RAG_EVALUATION_SCORE_PARSE_FAIL}。
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class RagEvaluationServiceImpl implements RagEvaluationService {

    /**
     * 评分提取正则：匹配 0-1 之间的浮点数
     */
    private static final Pattern SCORE_PATTERN = Pattern.compile("([01](?:\\.\\d+)?|0?\\.\\d+)");

    @Resource
    private AiRagEvaluationLogMapper evaluationLogMapper;

    /**
     * ChatClient Bean（可选；zhicloud-module-ai 未直接提供时为 null）
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * AiModelService（可选；用于在 ChatClient 不可用时按需构建 ChatModel）
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    @Override
    public RagEvaluationResult evaluate(RagEvaluationRequest request) {
        // 1. 校验上下文非空
        if (CollUtil.isEmpty(request.getContexts())) {
            throw exception(RAG_EVALUATION_CONTEXTS_EMPTY);
        }
        // 2. 顺序计算 4 项指标（避免 LLM 并发限流）
        Double faithfulness = evaluateFaithfulness(request.getAnswer(), request.getContexts());
        Double answerRelevancy = evaluateAnswerRelevancy(request.getQuestion(), request.getAnswer());
        Double contextPrecision = evaluateContextPrecision(request.getQuestion(), request.getContexts());
        Double contextRecall = null;
        if (StrUtil.isNotBlank(request.getGroundTruth())) {
            contextRecall = evaluateContextRecall(request.getQuestion(), request.getContexts(), request.getGroundTruth());
        }
        // 3. 综合得分（4 项指标的算术平均，缺失项不参与计算）
        double overall = average(faithfulness, answerRelevancy, contextPrecision, contextRecall);
        RagEvaluationResult result = new RagEvaluationResult()
                .setFaithfulness(faithfulness)
                .setAnswerRelevancy(answerRelevancy)
                .setContextPrecision(contextPrecision)
                .setContextRecall(contextRecall)
                .setOverallScore(overall);
        log.info("[evaluate][question={}, faithfulness={}, answerRelevancy={}, contextPrecision={}, contextRecall={}, overall={}]",
                request.getQuestion(), faithfulness, answerRelevancy, contextPrecision, contextRecall, overall);
        // 4. 记录评估日志
        saveEvaluationLogSafely(request, result);
        return result;
    }

    @Override
    public Double evaluateFaithfulness(String answer, List<String> contexts) {
        if (CollUtil.isEmpty(contexts)) {
            throw exception(RAG_EVALUATION_CONTEXTS_EMPTY);
        }
        String context = String.join("\n---\n", contexts);
        String systemPrompt = "你是一个 RAG 评估专家。请判断回答是否完全基于检索到的上下文，有无幻觉（编造上下文中不存在的信息）。" +
                "只返回一个 0 到 1 之间的浮点数，1 表示完全忠实于上下文，0 表示完全编造。不要输出任何其他内容。";
        String userPrompt = StrUtil.format("【检索上下文】\n{}\n\n【回答】\n{}\n\n请返回忠实度评分（0-1）：", context, answer);
        String raw = callLlm(systemPrompt, userPrompt);
        return parseScore(raw, "faithfulness");
    }

    @Override
    public Double evaluateAnswerRelevancy(String question, String answer) {
        String systemPrompt = "你是一个 RAG 评估专家。请判断回答是否切题，是否完整覆盖了用户问题的意图。" +
                "只返回一个 0 到 1 之间的浮点数，1 表示完全切题且完整，0 表示完全不相关。不要输出任何其他内容。";
        String userPrompt = StrUtil.format("【用户问题】\n{}\n\n【回答】\n{}\n\n请返回回答相关性评分（0-1）：", question, answer);
        String raw = callLlm(systemPrompt, userPrompt);
        return parseScore(raw, "answerRelevancy");
    }

    @Override
    public Double evaluateContextPrecision(String question, List<String> contexts) {
        if (CollUtil.isEmpty(contexts)) {
            throw exception(RAG_EVALUATION_CONTEXTS_EMPTY);
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            contextBuilder.append("[").append(i + 1).append("] ").append(contexts.get(i)).append("\n");
        }
        String systemPrompt = "你是一个 RAG 评估专家。请评估检索到的上下文文档与用户问题的相关性。" +
                "只返回一个 0 到 1 之间的浮点数，1 表示所有文档都高度相关，0 表示全部无关。不要输出任何其他内容。";
        String userPrompt = StrUtil.format("【用户问题】\n{}\n\n【检索到的上下文】\n{}\n请返回上下文精确率评分（0-1）：",
                question, contextBuilder);
        String raw = callLlm(systemPrompt, userPrompt);
        return parseScore(raw, "contextPrecision");
    }

    @Override
    public Double evaluateContextRecall(String question, List<String> contexts, String groundTruth) {
        if (CollUtil.isEmpty(contexts)) {
            throw exception(RAG_EVALUATION_CONTEXTS_EMPTY);
        }
        if (StrUtil.isBlank(groundTruth)) {
            throw exception(RAG_EVALUATION_GROUND_TRUTH_EMPTY);
        }
        String context = String.join("\n---\n", contexts);
        String systemPrompt = "你是一个 RAG 评估专家。请对比检索到的上下文与标准答案，判断上下文是否包含了标准答案中的所有关键信息。" +
                "只返回一个 0 到 1 之间的浮点数，1 表示完全召回（所有关键信息都在上下文中），0 表示完全未召回。不要输出任何其他内容。";
        String userPrompt = StrUtil.format(
                "【用户问题】\n{}\n\n【检索到的上下文】\n{}\n\n【标准答案】\n{}\n\n请返回上下文召回率评分（0-1）：",
                question, context, groundTruth);
        String raw = callLlm(systemPrompt, userPrompt);
        return parseScore(raw, "contextRecall");
    }

    // ==================== 内部方法 ====================

    /**
     * 调用 LLM 获取评分
     *
     * <p>优先使用 ChatClient Bean；不可用时通过 AiModelService 构建临时 ChatClient。
     */
    private String callLlm(String systemPrompt, String userPrompt) {
        if (chatClient != null) {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        }
        if (aiModelService != null) {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            ChatClient client = ChatClient.builder(chatModel).build();
            return client.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        }
        throw exception(RAG_EVALUATION_LLM_UNAVAILABLE);
    }

    /**
     * 解析 LLM 返回的评分（0-1 浮点）
     *
     * <p>LLM 可能返回 "0.85"、"[0.85]"、"评分：0.85" 等格式，用正则提取首个 0-1 浮点数。
     */
    private Double parseScore(String raw, String metricName) {
        if (StrUtil.isBlank(raw)) {
            throw exception(RAG_EVALUATION_SCORE_PARSE_FAIL, metricName + " 返回为空");
        }
        Matcher matcher = SCORE_PATTERN.matcher(raw.trim());
        if (!matcher.find()) {
            throw exception(RAG_EVALUATION_SCORE_PARSE_FAIL, metricName + " 原始返回=" + StrUtil.sub(raw, 0, 100));
        }
        try {
            double score = Double.parseDouble(matcher.group(1));
            // 截断到 [0, 1] 区间
            return Math.max(0.0, Math.min(1.0, score));
        } catch (NumberFormatException e) {
            throw exception(RAG_EVALUATION_SCORE_PARSE_FAIL, metricName + " 解析异常=" + matcher.group(1));
        }
    }

    /**
     * 计算指标的算术平均（缺失项不参与计算）
     */
    private double average(Double faithfulness, Double answerRelevancy,
                           Double contextPrecision, Double contextRecall) {
        double sum = 0;
        int count = 0;
        if (faithfulness != null) { sum += faithfulness; count++; }
        if (answerRelevancy != null) { sum += answerRelevancy; count++; }
        if (contextPrecision != null) { sum += contextPrecision; count++; }
        if (contextRecall != null) { sum += contextRecall; count++; }
        return count > 0 ? sum / count : 0.0;
    }

    /**
     * 安全保存评估日志（异常不影响主流程）
     */
    private void saveEvaluationLogSafely(RagEvaluationRequest request, RagEvaluationResult result) {
        try {
            AiRagEvaluationLogDO logDO = new AiRagEvaluationLogDO()
                    .setQuestion(request.getQuestion())
                    .setAnswer(request.getAnswer())
                    .setContextCount(CollUtil.size(request.getContexts()))
                    .setHasGroundTruth(StrUtil.isNotBlank(request.getGroundTruth()))
                    .setFaithfulness(result.getFaithfulness())
                    .setAnswerRelevancy(result.getAnswerRelevancy())
                    .setContextPrecision(result.getContextPrecision())
                    .setContextRecall(result.getContextRecall())
                    .setOverallScore(result.getOverallScore())
                    .setDetail(result.getDetail());
            evaluationLogMapper.insert(logDO);
        } catch (Exception e) {
            log.warn("[saveEvaluationLogSafely][保存评估日志失败，question={}]", request.getQuestion(), e);
        }
    }

}
