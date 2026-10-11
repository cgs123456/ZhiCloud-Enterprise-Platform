package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - HRM 首页薪资概览 VO")
@Data
public class HrmHrHomeSalarySurveyVO {

    @Schema(description = "月度工资表编号")
    private Long monthRecordId;

    @Schema(description = "计薪员工数量")
    private Long employeeCount;

    @Schema(description = "实发工资合计")
    private java.math.BigDecimal realPaySalary;

    @Schema(description = "部门薪资占比")
    private java.util.List<HrmHrHomeSalaryDeptVO> deptProportions;

}
