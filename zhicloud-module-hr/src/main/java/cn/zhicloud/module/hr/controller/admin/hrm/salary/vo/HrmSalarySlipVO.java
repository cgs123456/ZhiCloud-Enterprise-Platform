package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 工资条 VO")
@Data
public class HrmSalarySlipVO {

    @Schema(description = "工资条编号")
    private Long id;

    @Schema(description = "工资条发放记录编号")
    private Long sendRecordId;

    @Schema(description = "员工月度工资记录编号")
    private Long monthEmployeeRecordId;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "查看状态：0未查看 1已查看")
    private Integer readStatus;

    @Schema(description = "工资条项")
    private List<HrmSalarySlipOptionVO> options;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
