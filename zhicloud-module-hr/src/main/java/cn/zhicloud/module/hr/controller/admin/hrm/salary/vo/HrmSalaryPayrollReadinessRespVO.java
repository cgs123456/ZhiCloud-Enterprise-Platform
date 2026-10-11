package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 薪资核算就绪状态 VO")
@Data
public class HrmSalaryPayrollReadinessRespVO {

    @Schema(description = "月度工资表编号")
    private Long monthRecordId;

    @Schema(description = "工资表标题")
    private String title;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "社保年月")
    private String socialSecurityYearMonth;

    @Schema(description = "计薪人数")
    private Long payrollEmployeeCount;

    @Schema(description = "已定薪人数")
    private Long salaryEmployeeCount;

    @Schema(description = "未定薪人数")
    private Long noSalaryEmployeeCount;

    @Schema(description = "未分配薪资组人数")
    private Long noSalaryGroupEmployeeCount;

    @Schema(description = "异动人数")
    private Long changeEmployeeCount;

    @Schema(description = "异动类型数量")
    private Map<Integer, Long> changeTypeCountMap;

    @Schema(description = "未定薪员工")
    private List<HrmSalaryMonthEmployeeRecordVO> noSalaryEmployees;

}
