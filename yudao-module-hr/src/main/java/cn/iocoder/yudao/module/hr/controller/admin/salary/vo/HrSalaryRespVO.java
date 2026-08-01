package cn.iocoder.yudao.module.hr.controller.admin.salary.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.hr.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 薪资记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrSalaryRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @ExcelProperty("员工 ID")
    private Long employeeId;

    @Schema(description = "薪资月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    @ExcelProperty("薪资月份")
    private String salaryMonth;

    @Schema(description = "基本工资", example = "10000.00")
    @ExcelProperty("基本工资")
    private BigDecimal baseSalary;

    @Schema(description = "加班费", example = "500.00")
    @ExcelProperty("加班费")
    private BigDecimal overtimePay;

    @Schema(description = "奖金", example = "1000.00")
    @ExcelProperty("奖金")
    private BigDecimal bonus;

    @Schema(description = "扣款", example = "100.00")
    @ExcelProperty("扣款")
    private BigDecimal deduction;

    @Schema(description = "社保", example = "800.00")
    @ExcelProperty("社保")
    private BigDecimal socialInsurance;

    @Schema(description = "公积金", example = "600.00")
    @ExcelProperty("公积金")
    private BigDecimal housingFund;

    @Schema(description = "个税", example = "300.00")
    @ExcelProperty("个税")
    private BigDecimal tax;

    @Schema(description = "实发工资", example = "9700.00")
    @ExcelProperty("实发工资")
    private BigDecimal netSalary;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_SALARY_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}