package cn.zhicloud.module.aimultiagent.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 多 Agent 执行轨迹详情 VO（Span 列表 + 汇总）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 执行轨迹详情")
@Data
public class MultiAgentTraceRespVO {

    @Schema(description = "执行日志编号", example = "1024")
    private Long executionLogId;

    @Schema(description = "全链路追踪 ID", example = "3f7a9c2e-...")
    private String traceId;

    @Schema(description = "Span 列表（按开始时间升序）")
    private List<MultiAgentSpanRespVO> spans;

    @Schema(description = "轨迹汇总")
    private MultiAgentTraceSummaryVO summary;

}
