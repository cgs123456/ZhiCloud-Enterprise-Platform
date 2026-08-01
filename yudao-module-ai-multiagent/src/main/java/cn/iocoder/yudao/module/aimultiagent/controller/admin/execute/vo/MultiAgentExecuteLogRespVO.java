package cn.iocoder.yudao.module.aimultiagent.controller.admin.execute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 多 Agent 编排执行日志 Response VO")
@Data
public class MultiAgentExecuteLogRespVO {

    @Schema(description = "编号", required = true, example = "1")
    private Long id;

    @Schema(description = "拓扑 ID", required = true, example = "1")
    private Long topologyId;

    @Schema(description = "用户输入", required = true, example = "帮我生成一份库存报告")
    private String userInput;

    @Schema(description = "Supervisor 任务拆解 JSON", example = "[]")
    private String supervisorPlan;

    @Schema(description = "Worker 执行结果 JSON", example = "[]")
    private String workerResults;

    @Schema(description = "最终汇总答案", example = "本月库存报告...")
    private String finalAnswer;

    @Schema(description = "总 Token 消耗", required = true, example = "1024")
    private Integer totalTokens;

    @Schema(description = "实际调用深度", required = true, example = "3")
    private Integer actualDepth;

    @Schema(description = "执行状态（0进行中 1成功 2失败 3熔断）", required = true, example = "1")
    private Integer status;

    @Schema(description = "错误信息", example = "")
    private String errorMsg;

    @Schema(description = "执行耗时（毫秒）", required = true, example = "5000")
    private Long durationMs;

    @Schema(description = "创建时间", required = true, example = "时间戳格式")
    private LocalDateTime createTime;

}
