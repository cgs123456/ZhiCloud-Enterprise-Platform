package cn.iocoder.yudao.module.oa.controller.admin.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 会议室预约新增/修改 Request VO")
@Data
public class OaMeetingReservationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "会议主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "季度复盘会")
    @NotEmpty(message = "会议主题不能为空")
    private String title;

    @Schema(description = "会议室 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    @Schema(description = "组织人 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "组织人不能为空")
    private Long organizerUserId;

    @Schema(description = "参会人（逗号分隔）", example = "1,2,3")
    private String attendeeUserIds;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01T10:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01T11:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "状态（10 待确认 20 已确认 30 已取消 40 已完成）", example = "10")
    private Integer status;

    @Schema(description = "是否提醒", example = "true")
    private Boolean reminderEnabled;

    @Schema(description = "提前提醒分钟", example = "15")
    private Integer reminderMinutes;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
