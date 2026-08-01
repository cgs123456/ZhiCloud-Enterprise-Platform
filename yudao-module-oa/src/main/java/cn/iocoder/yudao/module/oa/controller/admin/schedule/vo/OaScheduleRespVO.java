package cn.iocoder.yudao.module.oa.controller.admin.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 日程 Response VO")
@Data
public class OaScheduleRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "标题", example = "项目评审会")
    private String title;

    @Schema(description = "描述", example = "讨论 Q4 方案")
    private String description;

    @Schema(description = "日程类型（10 日程 / 20 任务 / 30 纪念日 / 40 会议）", example = "40")
    private Integer type;

    @Schema(description = "开始时间", example = "2024-01-01T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2024-01-01T11:00:00")
    private LocalDateTime endTime;

    @Schema(description = "全天事件", example = "false")
    private Boolean allDay;

    @Schema(description = "地点", example = "3 楼会议室")
    private String location;

    @Schema(description = "提前提醒分钟数", example = "15")
    private Integer remindMinutes;

    @Schema(description = "是否已提醒", example = "false")
    private Boolean reminded;

    @Schema(description = "重复类型（0 不重复 / 10 每天 / 20 每周 / 30 每月 / 40 每年）", example = "0")
    private Integer repeatType;

    @Schema(description = "状态（0 未完成 / 1 已完成 / 2 已取消）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
