package cn.zhicloud.module.airag.service.eval;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RagEvalReportAggregator} 单元测试：聚合数学、退步判定、空值容错。
 *
 * @author zhicloud
 */
class RagEvalReportAggregatorTest {

    private static RagEvalReportAggregator.SingleResult r(Long id, Double f, Double rel, Double p, Double rc) {
        return new RagEvalReportAggregator.SingleResult(id, f, rel, p, rc);
    }

    @Test
    void aggregate_basicMath() {
        List<RagEvalReportAggregator.SingleResult> list = List.of(
                r(1L, 0.8, 0.9, 0.7, 0.6),
                r(2L, 1.0, 1.0, 1.0, 1.0));
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(list, null);
        assertEquals(0.9, agg.avgFaithfulness(), 1e-9);
        assertEquals(0.95, agg.avgAnswerRelevancy(), 1e-9);
        assertEquals(0.85, agg.avgContextPrecision(), 1e-9);
        assertEquals(0.8, agg.avgContextRecall(), 1e-9);
        assertEquals(0.875, agg.overallScore(), 1e-9);
        assertTrue(agg.regressedQuestionIds().isEmpty());
        assertTrue(agg.improvedQuestionIds().isEmpty());
    }

    @Test
    void aggregate_nullMetricsExcludedFromDenominator() {
        List<RagEvalReportAggregator.SingleResult> list = List.of(
                r(1L, null, null, null, null),
                r(2L, 0.6, null, null, null));
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(list, null);
        assertEquals(0.6, agg.avgFaithfulness(), 1e-9);
        assertNull(agg.avgAnswerRelevancy());
        assertEquals(0.6, agg.overallScore(), 1e-9);
    }

    @Test
    void aggregate_regressionAndImprovementDetection() {
        List<RagEvalReportAggregator.SingleResult> list = List.of(
                r(1L, 0.5, 0.5, 0.5, 0.5), // 相对基线 0.8 退步
                r(2L, 0.9, 0.9, 0.9, 0.9), // 相对基线 0.8 进步
                r(3L, 0.81, 0.81, 0.81, 0.81)); // 变化 0.01，不计
        Map<Long, Double> prev = new HashMap<>();
        prev.put(1L, 0.8);
        prev.put(2L, 0.8);
        prev.put(3L, 0.8);
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(list, prev);
        assertEquals(List.of(1L), agg.regressedQuestionIds());
        assertEquals(List.of(2L), agg.improvedQuestionIds());
    }

    @Test
    void aggregate_noBaseline_noRegressionLists() {
        List<RagEvalReportAggregator.SingleResult> list = List.of(r(1L, 0.2, 0.2, 0.2, 0.2));
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(list, Collections.emptyMap());
        assertTrue(agg.regressedQuestionIds().isEmpty());
        assertTrue(agg.improvedQuestionIds().isEmpty());
    }

    @Test
    void median_oddEvenEmpty() {
        assertEquals(0.5, RagEvalReportAggregator.median(List.of(0.1, 0.5, 0.9)), 1e-9);
        assertEquals(0.5, RagEvalReportAggregator.median(List.of(0.2, 0.8)), 1e-9);
        assertNull(RagEvalReportAggregator.median(Collections.emptyList()));
        assertNull(RagEvalReportAggregator.median(null));
    }

    @Test
    void singleOverall_allNull_returnsNull() {
        assertNull(r(9L, null, null, null, null).overall());
        assertEquals(1.0, r(9L, 1.0, 1.0, 1.0, 1.0).overall(), 1e-9);
    }

    @Test
    void aggregate_nullBaselineMap_noRegressionLists() {
        // 无基线（null）时不做退步/进步判定，但聚合值正常
        List<RagEvalReportAggregator.SingleResult> list = List.of(r(1L, 0.7, 0.7, 0.7, 0.7));
        RagEvalReportAggregator.AggregateResult agg =
                RagEvalReportAggregator.aggregate(list, null);
        assertEquals(0.7, agg.overallScore(), 1e-9);
        assertTrue(agg.regressedQuestionIds().isEmpty());
        assertTrue(agg.improvedQuestionIds().isEmpty());
    }

}
