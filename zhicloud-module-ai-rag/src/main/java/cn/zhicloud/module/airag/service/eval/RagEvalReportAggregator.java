package cn.zhicloud.module.airag.service.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量评估报告聚合器（纯函数，可单测）
 *
 * <p>职责：单题得分聚合（均值/中位数/最低分）+ 与上次报告对比（退步/进步判定）。
 * 与 DB/LLM 无关，便于单元测试覆盖聚合边界与对比逻辑。
 *
 * @author zhicloud
 */
public final class RagEvalReportAggregator {

    /**
     * 退步/进步判定阈值（单题综合分变化绝对值）
     */
    public static final double REGRESSION_THRESHOLD = 0.05;

    private RagEvalReportAggregator() {
    }

    /**
     * 单题评估结果（聚合输入）
     *
     * @param questionId      题目编号
     * @param faithfulness    忠实度（可为空，表示该题该指标未产出）
     * @param answerRelevancy 回答相关性
     * @param contextPrecision 上下文精确率
     * @param contextRecall   上下文召回率
     */
    public record SingleResult(Long questionId, Double faithfulness, Double answerRelevancy,
                               Double contextPrecision, Double contextRecall) {

        /**
         * 单题综合分（4 指标算术平均，空指标不计入分母；全空返回 null）
         */
        public Double overall() {
            double sum = 0.0;
            int count = 0;
            if (faithfulness != null) {
                sum += faithfulness;
                count++;
            }
            if (answerRelevancy != null) {
                sum += answerRelevancy;
                count++;
            }
            if (contextPrecision != null) {
                sum += contextPrecision;
                count++;
            }
            if (contextRecall != null) {
                sum += contextRecall;
                count++;
            }
            return count == 0 ? null : sum / count;
        }
    }

    /**
     * 聚合报告
     *
     * @param avgFaithfulness    忠实度均值（无有效值则为 null）
     * @param avgAnswerRelevancy 回答相关性均值
     * @param avgContextPrecision 上下文精确率均值
     * @param avgContextRecall   上下文召回率均值
     * @param overallScore       综合得分（4 均值再平均，无有效值则为 null）
     * @param regressedQuestionIds 退步题目编号
     * @param improvedQuestionIds 进步题目编号
     */
    public record AggregateResult(Double avgFaithfulness, Double avgAnswerRelevancy,
                                  Double avgContextPrecision, Double avgContextRecall,
                                  Double overallScore,
                                  List<Long> regressedQuestionIds, List<Long> improvedQuestionIds) {
    }

    /**
     * 聚合单题结果，并与上次各题综合分对比
     *
     * @param results 本次单题结果（不能为空）
     * @param previousOverallByQuestionId 上次各题综合分（questionId -&gt; overall，可为空表示无基线）
     * @return 聚合报告
     */
    public static AggregateResult aggregate(List<SingleResult> results,
                                            java.util.Map<Long, Double> previousOverallByQuestionId) {
        List<Double> faithfulness = new ArrayList<>();
        List<Double> relevancy = new ArrayList<>();
        List<Double> precision = new ArrayList<>();
        List<Double> recall = new ArrayList<>();
        List<Long> regressed = new ArrayList<>();
        List<Long> improved = new ArrayList<>();
        for (SingleResult r : results) {
            addIfPresent(faithfulness, r.faithfulness());
            addIfPresent(relevancy, r.answerRelevancy());
            addIfPresent(precision, r.contextPrecision());
            addIfPresent(recall, r.contextRecall());
            if (previousOverallByQuestionId != null) {
                Double prev = previousOverallByQuestionId.get(r.questionId());
                Double curr = r.overall();
                if (prev != null && curr != null) {
                    double delta = curr - prev;
                    if (delta <= -REGRESSION_THRESHOLD) {
                        regressed.add(r.questionId());
                    } else if (delta >= REGRESSION_THRESHOLD) {
                        improved.add(r.questionId());
                    }
                }
            }
        }
        Double avgF = avg(faithfulness);
        Double avgR = avg(relevancy);
        Double avgP = avg(precision);
        Double avgRc = avg(recall);
        Double overall = avgNonNull(avgF, avgR, avgP, avgRc);
        return new AggregateResult(avgF, avgR, avgP, avgRc, overall,
                Collections.unmodifiableList(regressed), Collections.unmodifiableList(improved));
    }

    /**
     * 中位数（空列表返回 null）
     */
    public static Double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static void addIfPresent(List<Double> list, Double v) {
        if (v != null) {
            list.add(v);
        }
    }

    private static Double avg(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (Double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private static Double avgNonNull(Double... values) {
        double sum = 0.0;
        int count = 0;
        for (Double v : values) {
            if (v != null) {
                sum += v;
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

}
