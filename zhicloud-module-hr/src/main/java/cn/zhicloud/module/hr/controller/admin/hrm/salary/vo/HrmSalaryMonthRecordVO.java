package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 月度工资表 VO")
@Data
public class HrmSalaryMonthRecordVO {

    @Schema(description = "月度工资表编号")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "计薪人数")
    private Integer employeeCount;

    @Schema(description = "计薪开始时间")
    private LocalDateTime startTime;

    @Schema(description = "计薪结束时间")
    private LocalDateTime endTime;

    @Schema(description = "应发工资")
    private BigDecimal expectedPaySalary;

    @Schema(description = "个人社保")
    private BigDecimal personalInsuranceAmount;

    @Schema(description = "个人公积金")
    private BigDecimal personalProvidentFundAmount;

    @Schema(description = "个人所得税")
    private BigDecimal personalTax;

    @Schema(description = "实发工资")
    private BigDecimal realPaySalary;

    @Schema(description = "公司社保")
    private BigDecimal corporateInsuranceAmount;

    @Schema(description = "公司公积金")
    private BigDecimal corporateProvidentFundAmount;

    @Schema(description = "工资表状态")
    private Integer status;

    @Schema(description = "工资项表头")
    private List<SalaryOptionValueVO> optionHeaders;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
