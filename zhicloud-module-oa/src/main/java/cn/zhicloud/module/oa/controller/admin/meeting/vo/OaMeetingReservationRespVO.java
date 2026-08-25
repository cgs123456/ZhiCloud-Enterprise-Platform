package cn.zhicloud.module.oa.controller.admin.meeting.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 会议室预约 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OaMeetingReservationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "会议主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "季度复盘会")
    @ExcelProperty("会议主题")
    private String title;

    @Schema(description = "会议室 ID", example = "2048")
    @ExcelProperty("会议室 ID")
    private Long roomId;

    @Schema(description = "组织人 ID", example = "2048")
    @ExcelProperty("组织人 ID")
    private Long organizerUserId;

    @Schema(description = "参会人（逗号分隔）", example = "1,2,3")
    @ExcelProperty("参会人")
    private String attendeeUserIds;

    @Schema(description = "开始时间", example = "2024-01-01T10:00:00")
    @ExcelProperty("开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2024-01-01T11:00:00")
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态（10 待确认 20 已确认 30 已取消 40 已完成）", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "是否提醒", example = "true")
    @ExcelProperty("是否提醒")
    private Boolean reminderEnabled;

    @Schema(description = "提前提醒分钟", example = "15")
    @ExcelProperty("提前提醒分钟")
    private Integer reminderMinutes;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
