package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工字段显示配置保存 Request VO")
@Data
public class HrmEmployeeFieldVisibleSaveReqVO {

    @Schema(description = "字段名称")
    @NotEmpty(message = "字段名称不能为空")
    private String name;

    @Schema(description = "是否显示")
    @NotNull(message = "是否显示不能为空")
    private Boolean visible;

    @Schema(description = "是否允许员工编辑（仅档案字段配置使用）")
    private Boolean editable;

}
