package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 首页部门薪资占比 VO")
@Data
public class HrmHrHomeSalaryDeptVO {

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "薪资占比（百分比）")
    private java.math.BigDecimal proportion;

    @Schema(description = "实发工资")
    private java.math.BigDecimal totalSalary;

}
