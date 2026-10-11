package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - HRM 档案字段配置保存 Request VO")
@Data
public class HrmEmployeeArchiveFieldConfigSaveReqVO {

    @Schema(description = "字段配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "字段列表不能为空")
    @Valid
    private List<HrmEmployeeFieldVisibleSaveReqVO> fields;

}
