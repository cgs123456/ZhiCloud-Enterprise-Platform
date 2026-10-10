package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套初始化 Request VO")
@Data
public class FmsAccountSetInitializeReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "本位币编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNY")
    @NotBlank(message = "本位币编码不能为空")
    private String currencyCode;

    @Schema(description = "启用期间，格式 yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED, example = "202601")
    @NotNull(message = "启用期间不能为空")
    private Integer startTime;

    @Schema(description = "会计制度", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会计制度不能为空")
    private Integer standard;

    @Schema(description = "科目层级", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "科目层级不能为空")
    private Integer level;

    @Schema(description = "科目编码规则", requiredMode = Schema.RequiredMode.REQUIRED, example = "4-2-2-2")
    @NotBlank(message = "科目编码规则不能为空")
    private String subjectCodeRule;

    @Schema(description = "账簿余额方向模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "账簿余额方向模式不能为空")
    private Integer ledgerBalanceMode;

}
