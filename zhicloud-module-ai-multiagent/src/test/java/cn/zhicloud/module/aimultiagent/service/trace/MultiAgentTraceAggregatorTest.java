package cn.zhicloud.module.aimultiagent.service.trace;

import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionSpanDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService.STATUS_FAILED;
import static cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService.STATUS_RUNNING;
import static cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService.STATUS_SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MultiAgentTraceAggregator} 单元测试：覆盖空列表 / 混合状态 /
 * Token 与耗时加和 / 墙钟跨度 / 时间缺失。
 *
 * @author zhicloud
 */
class MultiAgentTraceAggregatorTest {

    private static MultiAgentExecutionSpanDO span(Integer status, Integer tokens, Long durationMs,
                                                  LocalDateTime start, LocalDateTime end) {
        MultiAgentExecutionSpanDO span = new MultiAgentExecutionSpanDO();
        span.setStatus(status);
        span.setTokens(tokens);
        span.setDurationMs(durationMs);
        span.setStartTime(start);
        span.setEndTime(end);
        return span;
    }

    @Test
    void summarize_empty_returnsZero() {
        TraceSummary summary = MultiAgentTraceAggregator.summarize(Collections.emptyList());
        assertEquals(0, summary.getSpanCount());
        assertEquals(0, summary.getSuccessCount());
        assertEquals(0, summary.getFailedCount());
        assertEquals(0, summary.getRunningCount());
        assertEquals(0, summary.getTotalTokens());
        assertEquals(0L, summary.getTotalDurationMs());
        assertNull(summary.getWallDurationMs());
    }

    @Test
    void summarize_null_returnsZero() {
        TraceSummary summary = MultiAgentTraceAggregator.summarize(null);
        assertEquals(0, summary.getSpanCount());
        assertNull(summary.getWallDurationMs());
    }

    @Test
    void summarize_mixed_countsAndSums() {
        LocalDateTime t0 = LocalDateTime.of(2026, 9, 8, 10, 0, 0);
        LocalDateTime t1 = t0.plusSeconds(2);
        LocalDateTime t2 = t0.plusSeconds(5);
        LocalDateTime t3 = t0.plusSeconds(9);
        TraceSummary summary = MultiAgentTraceAggregator.summarize(Arrays.asList(
                span(STATUS_SUCCESS, 100, 2000L, t0, t1),
                span(STATUS_SUCCESS, 200, 3000L, t1, t2),
                span(STATUS_FAILED, 50, 4000L, t2, t3)));
        assertEquals(3, summary.getSpanCount());
        assertEquals(2, summary.getSuccessCount());
        assertEquals(1, summary.getFailedCount());
        assertEquals(0, summary.getRunningCount());
        assertEquals(350, summary.getTotalTokens());
        assertEquals(9000L, summary.getTotalDurationMs());
        assertEquals(9000L, summary.getWallDurationMs());
    }

    @Test
    void summarize_runningCountedSeparately() {
        TraceSummary summary = MultiAgentTraceAggregator.summarize(Arrays.asList(
                span(STATUS_SUCCESS, 10, 100L, null, null),
                span(STATUS_RUNNING, null, null, null, null)));
        assertEquals(2, summary.getSpanCount());
        assertEquals(1, summary.getSuccessCount());
        assertEquals(0, summary.getFailedCount());
        assertEquals(1, summary.getRunningCount());
        assertEquals(10, summary.getTotalTokens());
        // 时间缺失时墙钟跨度为 null，耗时加和不受影响
        assertNull(summary.getWallDurationMs());
        assertEquals(100L, summary.getTotalDurationMs());
    }

    @Test
    void summarize_nullFields_tolerated() {
        TraceSummary summary = MultiAgentTraceAggregator.summarize(
                Collections.singletonList(span(STATUS_SUCCESS, null, null, null, null)));
        assertEquals(1, summary.getSpanCount());
        assertEquals(0, summary.getTotalTokens());
        assertEquals(0L, summary.getTotalDurationMs());
        assertNull(summary.getWallDurationMs());
    }

}
