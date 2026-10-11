package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤打卡 Response VO")
@Data
public class HrmAttendanceClockRespVO {

    @Schema(description = "打卡记录编号")
    private Long id;

    @Schema(description = "打卡员工编号")
    private Long employeeId;

    @Schema(description = "打卡时间")
    private LocalDateTime clockTime;

    @Schema(description = "打卡类型：1上班 2下班")
    private Integer type;

    @Schema(description = "应打卡日期（当天）")
    private LocalDateTime attendanceTime;

    @Schema(description = "打卡来源：1手机端 2手工录入")
    private Integer sourceType;

    @Schema(description = "打卡状态（hrm_attendance_clock_status 字典）")
    private Integer status;

    @Schema(description = "打卡阶段")
    private Integer stage;

    @Schema(description = "打卡地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "WiFi 名称")
    private String ssid;

    @Schema(description = "WiFi MAC 地址")
    private String mac;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
