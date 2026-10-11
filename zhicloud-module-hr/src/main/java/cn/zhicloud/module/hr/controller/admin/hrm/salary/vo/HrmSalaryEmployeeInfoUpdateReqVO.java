package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工薪资档案修改 Request VO")
@Data
public class HrmSalaryEmployeeInfoUpdateReqVO {

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

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

}
