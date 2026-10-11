package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工薪资档案 VO")
@Data
public class HrmSalaryEmployeeInfoVO {

    @Schema(description = "员工薪资信息编号")
    private Long id;

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

    @Schema(description = "入职状态：1在职 2待入职 3待离职 4离职")
    private Integer entryStatus;

    @Schema(description = "员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包")
    private Integer status;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

    @Schema(description = "转正时间")
    private LocalDateTime regularTime;

    @Schema(description = "调薪原因")
    private Integer changeReason;

    @Schema(description = "调薪生效时间")
    private LocalDateTime effectTime;

    @Schema(description = "调薪类型")
    private Integer changeType;

    @Schema(description = "试用期工资")
    private BigDecimal probationSalary;

    @Schema(description = "转正工资")
    private BigDecimal regularSalary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "转正工资项")
    private List<SalaryOptionValueVO> salaryOptions;

    @Schema(description = "试用期工资项")
    private List<SalaryOptionValueVO> probationSalaryOptions;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
