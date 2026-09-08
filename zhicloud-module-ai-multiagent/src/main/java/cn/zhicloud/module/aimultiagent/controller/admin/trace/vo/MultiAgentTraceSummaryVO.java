package cn.zhicloud.module.aimultiagent.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 多 Agent 执行轨迹汇总 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 执行轨迹汇总")
@Data
public class MultiAgentTraceSummaryVO {

    @Schema(description = "Span 总数", example = "5")
    private int spanCount;

    @Schema(description = "成功数", example = "4")
    private int successCount;

    @Schema(description = "失败数", example = "1")
    private int failedCount;

    @Schema(description = "运行中数量", example = "0")
    private int runningCount;

    @Schema(description = "Token 总消耗", example = "1024")
    private int totalTokens;

    @Schema(description = "各 Span 耗时加和（毫秒）", example = "15000")
    private long totalDurationMs;

    @Schema(description = "墙钟跨度（毫秒）", example = "12000")
    private Long wallDurationMs;

}
