package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 每日打卡概况 Response VO")
@Data
public class HrmAttendanceDailyOverviewRespVO {

    @Schema(description = "打卡记录")
    private List<HrmAttendanceClockRespVO> clocks;

    @Schema(description = "考勤结果")
    private String attendanceResult;

    @Schema(description = "打卡概况展示项")
    private List<HrmAttendanceDailyOverviewItemRespVO> overviews;

}
