package cn.zhicloud.module.airag.evaluation;

import java.util.List;

/**
 * RAG 评估 Service 接口
 *
 * <p>评估 RAG 检索质量，包含 4 项核心指标：
 * <ul>
 *   <li>Faithfulness（忠实度）：回答是否基于检索到的文档</li>
 *   <li>Answer Relevancy（回答相关性）：回答是否切题</li>
 *   <li>Context Precision（上下文精确率）：检索到的文档是否相关</li>
 *   <li>Context Recall（上下文召回率）：是否检索到所有相关文档</li>
 * </ul>
 *
 * <p>评估方法：调用 ChatClient 用特定 prompt 让 LLM 打分（0-1 浮点）。
 *
 * @author zhicloud
 */
public interface RagEvaluationService {

    /**
     * 综合评估（一次性计算 4 项指标）
     *
     * @param request 评估请求（含 question/answer/contexts/groundTruth）
     * @return 评估结果
     */
    RagEvaluationResult evaluate(RagEvaluationRequest request);

    /**
     * 评估忠实度：回答是否忠实于检索到的上下文
     *
     * @param answer RAG 生成的回答
     * @param contexts 检索到的上下文文档列表
     * @return 忠实度得分（0-1，1 表示完全忠实）
     */
    Double evaluateFaithfulness(String answer, List<String> contexts);

    /**
     * 评估回答相关性：回答是否切题
     *
     * @param question 用户问题
     * @param answer RAG 生成的回答
     * @return 相关性得分（0-1，1 表示完全切题）
     */
    Double evaluateAnswerRelevancy(String question, String answer);

    /**
     * 评估上下文精确率：检索到的文档是否与问题相关
     *
     * @param question 用户问题
     * @param contexts 检索到的上下文文档列表
     * @return 精确率得分（0-1，1 表示所有文档都相关）
     */
    Double evaluateContextPrecision(String question, List<String> contexts);

    /**
     * 评估上下文召回率：是否检索到所有相关信息
     *
     * @param question 用户问题
     * @param contexts 检索到的上下文文档列表
     * @param groundTruth 标准答案
     * @return 召回率得分（0-1，1 表示完全召回）
     */
    Double evaluateContextRecall(String question, List<String> contexts, String groundTruth);

}
