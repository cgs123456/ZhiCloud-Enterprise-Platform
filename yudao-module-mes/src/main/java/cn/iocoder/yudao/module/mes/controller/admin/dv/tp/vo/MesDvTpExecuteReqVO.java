package cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES TPM 计划执行 Request VO")
@Data
public class MesDvTpExecuteReqVO {

    @Schema(description = "TPM 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "TPM 计划编号不能为空")
    private Long planId;

    @Schema(description = "执行日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行日期不能为空")
    private LocalDate executeDate;

    @Schema(description = "整体结果", example = "10")
    private Integer result;

    @Schema(description = "发现问题", example = "无")
    private String issuesFound;

    @Schema(description = "已采取措施", example = "已清洁")
    private String actionTaken;

    @Schema(description = "备注", example = "备注")
    private String remark;

}