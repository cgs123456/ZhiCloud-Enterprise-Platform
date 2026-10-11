package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工月度工资 VO")
@Data
public class HrmSalaryMonthEmployeeRecordVO {

    @Schema(description = "员工月度工资记录编号")
    private Long id;

    @Schema(description = "月度工资表编号")
    private Long monthRecordId;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "实际出勤天数")
    private BigDecimal actualWorkDay;

    @Schema(description = "应出勤天数")
    private BigDecimal needWorkDay;

    @Schema(description = "应发工资")
    private BigDecimal expectedPaySalary;

    @Schema(description = "应税工资")
    private BigDecimal taxableSalary;

    @Schema(description = "个人所得税")
    private BigDecimal personalTax;

    @Schema(description = "实发工资")
    private BigDecimal realPaySalary;

    @Schema(description = "绩效系数")
    private BigDecimal performanceCoefficient;

    @Schema(description = "工资项值")
    private List<SalaryOptionValueVO> optionValues;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
