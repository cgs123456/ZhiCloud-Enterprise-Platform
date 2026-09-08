package cn.zhicloud.module.aimultiagent.service.trace;

import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionSpanDO;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService.STATUS_FAILED;
import static cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService.STATUS_SUCCESS;

/**
 * 执行轨迹聚合器（纯函数，无 DB 依赖，可单测）
 *
 * <p>对一次执行的 Span 列表做汇总：成功/失败计数、Token 加和、
 * 各 Span 耗时加和与墙钟跨度（最早开始 → 最晚结束）。
 *
 * @author zhicloud
 */
public class MultiAgentTraceAggregator {

    private MultiAgentTraceAggregator() {
    }

    /**
     * 汇总 Span 列表
     *
     * @param spans Span 列表（可为空）
     * @return 汇总结果
     */
    public static TraceSummary summarize(List<MultiAgentExecutionSpanDO> spans) {
        if (spans == null || spans.isEmpty()) {
            return TraceSummary.builder()
                    .spanCount(0).successCount(0).failedCount(0).runningCount(0)
                    .totalTokens(0).totalDurationMs(0L).wallDurationMs(null)
                    .build();
        }
        int success = 0;
        int failed = 0;
        int tokens = 0;
        long durationSum = 0L;
        LocalDateTime earliest = null;
        LocalDateTime latest = null;
        for (MultiAgentExecutionSpanDO span : spans) {
            if (span == null) {
                continue;
            }
            if (Integer.valueOf(STATUS_SUCCESS).equals(span.getStatus())) {
                success++;
            } else if (Integer.valueOf(STATUS_FAILED).equals(span.getStatus())) {
                failed++;
            }
            if (span.getTokens() != null) {
                tokens += span.getTokens();
            }
            if (span.getDurationMs() != null) {
                durationSum += span.getDurationMs();
            }
            if (span.getStartTime() != null
                    && (earliest == null || span.getStartTime().isBefore(earliest))) {
                earliest = span.getStartTime();
            }
            if (span.getEndTime() != null
                    && (latest == null || span.getEndTime().isAfter(latest))) {
                latest = span.getEndTime();
            }
        }
        Long wall = null;
        if (earliest != null && latest != null && !latest.isBefore(earliest)) {
            wall = ChronoUnit.MILLIS.between(earliest, latest);
        }
        return TraceSummary.builder()
                .spanCount(spans.size())
                .successCount(success)
                .failedCount(failed)
                .runningCount(spans.size() - success - failed)
                .totalTokens(tokens)
                .totalDurationMs(durationSum)
                .wallDurationMs(wall)
                .build();
    }

}
