package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 员工绩效档案 VO（归档员工维度聚合）")
@Data
public class HrmPerformanceArchiveEmployeeVO {

    @Schema(description = "员工编号")
    private Long employeeId;

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

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "员工状态")
    private Integer employeeStatus;

    @Schema(description = "聘用形式")
    private Integer employeeType;

    @Schema(description = "最近考核编号")
    private Long latestAssessmentId;

    @Schema(description = "最近考核计划")
    private String latestPlanName;

    @Schema(description = "最近绩效评分")
    private BigDecimal latestScore;

    @Schema(description = "最近绩效等级")
    private String latestResultLevel;

    @Schema(description = "考核次数")
    private Long assessmentCount;

}
