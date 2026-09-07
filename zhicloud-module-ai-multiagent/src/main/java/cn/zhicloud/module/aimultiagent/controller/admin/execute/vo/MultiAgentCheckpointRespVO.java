package cn.zhicloud.module.aimultiagent.controller.admin.execute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 多 Agent 编排检查点 Response VO")
@Data
public class MultiAgentCheckpointRespVO {

    @Schema(description = "编号", required = true, example = "1")
    private Long id;

    @Schema(description = "关联执行日志编号", required = true, example = "10")
    private Long executionLogId;

    @Schema(description = "拓扑 ID", required = true, example = "1")
    private Long topologyId;

    @Schema(description = "检查点类型（PLAN_COMPLETED/WORKER_DONE/SUMMARIZE_DONE）", required = true,
            example = "WORKER_DONE")
    private String checkpointType;

    @Schema(description = "Worker 名称（WORKER_DONE 时有效）", example = "procurement-worker")
    private String workerName;

    @Schema(description = "任务序号（从 0 开始，WORKER_DONE 时有效）", example = "2")
    private Integer taskIndex;

    @Schema(description = "创建时间", required = true, example = "时间戳格式")
    private LocalDateTime createTime;

}
