package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 薪资项显示更新 Request VO")
@Data
public class HrmSalaryOptionVisibleReqVO {

    @Schema(description = "薪资项编号")
    @NotNull(message = "薪资项编号不能为空")
    private Long id;

    @Schema(description = "是否显示")
    @NotNull(message = "是否显示不能为空")
    private Boolean visible;

}
