package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工自助端 - 考勤打卡记录 VO
 *
 * 对齐前端 api/hrm/portal/attendance/clock AttendanceRecordVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 考勤打卡记录 VO")
@Data
public class HrmPortalAttendanceClockVO {

    @Schema(description = "打卡记录编号")
    private Long id;

    @Schema(description = "打卡员工编号")
    private Long employeeId;

    @Schema(description = "打卡时间")
    private LocalDateTime clockTime;

    @Schema(description = "打卡类型：1上班 2下班")
    private Integer type;

    @Schema(description = "应打卡时间")
    private LocalDateTime attendanceTime;

    @Schema(description = "打卡来源：1手机端 2手工录入")
    private Integer sourceType;

    @Schema(description = "打卡状态：1正常 2迟到 3早退 4缺卡 5旷工")
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

}
