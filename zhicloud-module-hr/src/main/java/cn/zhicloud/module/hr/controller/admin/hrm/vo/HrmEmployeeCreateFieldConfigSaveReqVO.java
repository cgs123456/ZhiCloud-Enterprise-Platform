package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - HRM 新建员工字段配置保存 Request VO")
@Data
public class HrmEmployeeCreateFieldConfigSaveReqVO {

    @Schema(description = "入职状态：1在职 2待入职", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职状态不能为空")
    private Integer entryStatus;

    @Schema(description = "字段显示配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "字段列表不能为空")
    @Valid
    private List<HrmEmployeeFieldVisibleSaveReqVO> fields;

}
