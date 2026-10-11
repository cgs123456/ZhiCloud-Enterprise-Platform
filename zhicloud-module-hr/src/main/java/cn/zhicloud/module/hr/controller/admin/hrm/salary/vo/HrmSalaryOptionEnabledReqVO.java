package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 薪资项启用更新 Request VO")
@Data
public class HrmSalaryOptionEnabledReqVO {

    @Schema(description = "薪资项编号")
    @NotNull(message = "薪资项编号不能为空")
    private Long id;

    @Schema(description = "是否启用")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

}
