package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 月度考勤详情 Response VO")
@Data
public class HrmAttendanceMonthDetailRespVO {

    @Schema(description = "月度汇总")
    private HrmAttendanceMonthRecordRespVO summary;

    @Schema(description = "每日明细")
    private List<HrmAttendanceDailyDetailRespVO> dailyDetails;

    @Schema(description = "请假记录")
    private List<HrmAttendanceLeaveRespVO> leaves;

}
