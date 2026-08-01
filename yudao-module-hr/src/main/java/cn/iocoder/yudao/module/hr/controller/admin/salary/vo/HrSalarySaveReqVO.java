package cn.iocoder.yudao.module.hr.controller.admin.salary.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.hr.enums.salary.HrSalaryStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 薪资记录新增/修改 Request VO")
@Data
public class HrSalarySaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "薪资月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    @NotEmpty(message = "薪资月份不能为空")
    private String salaryMonth;

    @Schema(description = "基本工资", example = "10000.00")
    private BigDecimal baseSalary;

    @Schema(description = "加班费", example = "500.00")
    private BigDecimal overtimePay;

    @Schema(description = "奖金", example = "1000.00")
    private BigDecimal bonus;

    @Schema(description = "扣款", example = "100.00")
    private BigDecimal deduction;

    @Schema(description = "社保", example = "800.00")
    private BigDecimal socialInsurance;

    @Schema(description = "公积金", example = "600.00")
    private BigDecimal housingFund;

    @Schema(description = "个税", example = "300.00")
    private BigDecimal tax;

    @Schema(description = "实发工资", example = "9700.00")
    private BigDecimal netSalary;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    @InEnum(HrSalaryStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}