package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工参保方案更新 Request VO")
@Data
public class HrmInsuranceEmployeeUpdateSchemeReqVO {

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "社保方案编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "社保方案编号不能为空")
    private Long schemeId;

}
