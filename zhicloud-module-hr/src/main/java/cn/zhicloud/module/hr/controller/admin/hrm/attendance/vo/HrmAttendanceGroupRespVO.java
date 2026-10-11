package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 考勤组 Response VO（班次/地点/WiFi/特殊日期/扣款规则嵌套）")
@Data
public class HrmAttendanceGroupRespVO {

    @Schema(description = "考勤组编号")
    private Long id;

    @Schema(description = "考勤组名称")
    private String name;

    @Schema(description = "是否启用 WiFi 打卡")
    private Boolean openWifiCard;

    @Schema(description = "是否启用定位打卡")
    private Boolean openPointCard;

    @Schema(description = "是否法定节假日休息")
    private Boolean rest;

    @Schema(description = "是否默认考勤组")
    private Boolean defaultStatus;

    @Schema(description = "特殊日期配置数组 [{type,date}]")
    private List<Object> specialDates;

    @Schema(description = "适用部门编号数组")
    private List<Long> deptIds;

    @Schema(description = "适用部门名称数组（enrich）")
    private List<String> deptNames;

    @Schema(description = "适用员工编号数组")
    private List<Long> employeeIds;

    @Schema(description = "适用员工名称数组（enrich）")
    private List<String> employeeNames;

    @Schema(description = "班次配置数组")
    private List<Object> shifts;

    @Schema(description = "打卡地点数组")
    private List<Object> points;

    @Schema(description = "打卡 WiFi 数组")
    private List<Object> wifis;

    @Schema(description = "扣款规则对象")
    private Object deductRule;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
