package cn.zhicloud.module.aimultiagent.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 多 Agent 执行 Span 响应 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 执行 Span")
@Data
public class MultiAgentSpanRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "执行日志编号", example = "1024")
    private Long executionLogId;

    @Schema(description = "Span 类型（PLAN/WORKER/SUMMARIZE）", example = "WORKER")
    private String spanType;

    @Schema(description = "Worker 名称", example = "procurement-worker")
    private String workerName;

    @Schema(description = "任务序号", example = "0")
    private Integer taskIndex;

    @Schema(description = "任务 ID", example = "task-1")
    private String taskId;

    @Schema(description = "状态（0运行中 1成功 2失败）", example = "1")
    private Integer status;

    @Schema(description = "消耗 Token 数", example = "128")
    private Integer tokens;

    @Schema(description = "耗时（毫秒）", example = "3200")
    private Long durationMs;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "输出摘要")
    private String outputExcerpt;

}
