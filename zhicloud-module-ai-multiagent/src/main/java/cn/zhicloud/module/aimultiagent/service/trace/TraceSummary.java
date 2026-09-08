package cn.zhicloud.module.aimultiagent.service.trace;

import lombok.Builder;
import lombok.Data;

/**
 * 执行轨迹汇总（纯值对象，由 {@link MultiAgentTraceAggregator} 计算）
 *
 * @author zhicloud
 */
@Data
@Builder
public class TraceSummary {

    /**
     * Span 总数
     */
    private int spanCount;
    /**
     * 成功数
     */
    private int successCount;
    /**
     * 失败数
     */
    private int failedCount;
    /**
     * 运行中数量（执行被中断时大于 0）
     */
    private int runningCount;
    /**
     * Token 总消耗（null 按 0 计）
     */
    private int totalTokens;
    /**
     * 各 Span 耗时加和（毫秒，null 按 0 计）
     */
    private long totalDurationMs;
    /**
     * 墙钟跨度（最早开始 → 最晚结束，毫秒；时间缺失时为 null）
     */
    private Long wallDurationMs;

}
