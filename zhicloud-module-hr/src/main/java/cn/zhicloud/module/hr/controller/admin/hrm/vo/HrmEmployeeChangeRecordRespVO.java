package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工异动记录 Response VO")
@Data
public class HrmEmployeeChangeRecordRespVO {

    @Schema(description = "异动记录编号")
    private Long id;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "异动类型：4转正 5调岗 6晋升 7降级 8转为全职 9再入职")
    private Integer type;

    @Schema(description = "异动原因")
    private Integer reason;

    @Schema(description = "原部门编号")
    private Long oldDeptId;

    @Schema(description = "原部门名称")
    private String oldDeptName;

    @Schema(description = "新部门编号")
    private Long newDeptId;

    @Schema(description = "新部门名称")
    private String newDeptName;

    @Schema(description = "原职位")
    private String oldPostName;

    @Schema(description = "新职位")
    private String newPostName;

    @Schema(description = "原岗位职级")
    private String oldPostLevel;

    @Schema(description = "新岗位职级")
    private String newPostLevel;

    @Schema(description = "原工作地点")
    private String oldWorkAddress;

    @Schema(description = "新工作地点")
    private String newWorkAddress;

    @Schema(description = "原直属上级员工编号")
    private Long oldLeaderEmployeeId;

    @Schema(description = "原直属上级员工姓名")
    private String oldLeaderEmployeeName;

    @Schema(description = "新直属上级员工编号")
    private Long newLeaderEmployeeId;

    @Schema(description = "新直属上级员工姓名")
    private String newLeaderEmployeeName;

    @Schema(description = "试用期（月）")
    private Integer probation;

    @Schema(description = "生效日期")
    private LocalDateTime effectTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
