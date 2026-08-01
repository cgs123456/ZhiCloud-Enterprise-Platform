package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 面试结果记录 Request VO")
@Data
public class HrInterviewResultReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "结果 1通过 2待定 3不通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "面试结果不能为空")
    private Integer result;

    @Schema(description = "面试评价", example = "表现优秀")
    private String comment;

}