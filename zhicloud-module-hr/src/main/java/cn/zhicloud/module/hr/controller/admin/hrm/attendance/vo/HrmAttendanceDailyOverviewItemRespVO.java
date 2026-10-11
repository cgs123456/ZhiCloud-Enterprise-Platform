package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 每日打卡概况展示项 Response VO")
@Data
public class HrmAttendanceDailyOverviewItemRespVO {

    @Schema(description = "打卡类型名称")
    private String type;

    @Schema(description = "打卡时间")
    private String time;

    @Schema(description = "打卡状态名称")
    private String status;

    @Schema(description = "考勤结果")
    private String text;

}
