package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 简历筛选 Request VO")
@Data
public class HrResumeScreenReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "是否通过 true通过 false淘汰", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "筛选结果不能为空")
    private Boolean passed;

    @Schema(description = "备注", example = "符合要求")
    private String remark;

}