package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工参保信息 VO")
@Data
public class HrmInsuranceEmployeeInfoVO {

    @Schema(description = "员工参保信息编号")
    private Long id;

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "是否本地首次缴纳社保")
    private Boolean firstSocialSecurity;

    @Schema(description = "是否本地首次缴纳公积金")
    private Boolean firstAccumulationFund;

    @Schema(description = "社保账号")
    private String socialSecurityNumber;

    @Schema(description = "公积金账号")
    private String accumulationFundNumber;

    @Schema(description = "社保起缴月份")
    private LocalDateTime socialSecurityStartMonth;

    @Schema(description = "社保方案编号")
    private Long schemeId;

    @Schema(description = "社保方案名称")
    private String schemeName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
