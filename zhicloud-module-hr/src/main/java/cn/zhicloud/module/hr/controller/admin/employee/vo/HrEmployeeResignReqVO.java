package cn.zhicloud.module.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 员工离职 Request VO")
@Data
public class HrEmployeeResignReqVO {

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "员工编号不能为空")
    private Long id;

    @Schema(description = "离职日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-12-31")
    @NotNull(message = "离职日期不能为空")
    private LocalDate leaveDate;

}