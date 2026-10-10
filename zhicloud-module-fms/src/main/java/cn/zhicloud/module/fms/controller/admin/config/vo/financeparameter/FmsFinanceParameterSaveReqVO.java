package cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套参数修改 Request VO")
@Data
public class FmsFinanceParameterSaveReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "会计制度")
    private Integer standard;

    @Schema(description = "科目层级", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "科目层级不能为空")
    private Integer level;

    @Schema(description = "科目编码规则", requiredMode = Schema.RequiredMode.REQUIRED, example = "4-2-2-2")
    @NotNull(message = "科目编码规则不能为空")
    private String subjectCodeRule;

    @Schema(description = "账簿余额方向模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "账簿余额方向模式不能为空")
    private Integer ledgerBalanceMode;

    @Schema(description = "结账前是否要求凭证审核", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结账前是否要求凭证审核不能为空")
    private Boolean voucherReviewRequired;

}
