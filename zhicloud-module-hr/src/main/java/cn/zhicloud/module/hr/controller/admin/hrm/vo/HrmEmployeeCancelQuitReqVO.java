package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工取消离职 Request VO")
@Data
public class HrmEmployeeCancelQuitReqVO {

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "取消原因")
    @NotEmpty(message = "取消原因不能为空")
    private String reason;

}
