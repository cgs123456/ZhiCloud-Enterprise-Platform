package cn.zhicloud.module.hr.controller.admin.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 薪资月度核算 Request VO")
@Data
public class HrSalaryCalculateReqVO {

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "薪资月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    @NotEmpty(message = "薪资月份不能为空")
    private String salaryMonth;

    @Schema(description = "奖金", example = "1000.00")
    private BigDecimal bonus;

    @Schema(description = "扣款", example = "100.00")
    private BigDecimal deduction;

    @Schema(description = "社保", example = "800.00")
    private BigDecimal socialInsurance;

    @Schema(description = "公积金", example = "600.00")
    private BigDecimal housingFund;

}