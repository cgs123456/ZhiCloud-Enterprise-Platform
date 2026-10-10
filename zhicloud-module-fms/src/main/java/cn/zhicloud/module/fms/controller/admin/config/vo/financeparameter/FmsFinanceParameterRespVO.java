package cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套参数 Response VO")
@Data
public class FmsFinanceParameterRespVO {

    @Schema(description = "账套编号", example = "1")
    private Long accountSetId;

    @Schema(description = "会计制度")
    private Integer standard;

    @Schema(description = "科目层级")
    private Integer level;

    @Schema(description = "科目编码规则")
    private String subjectCodeRule;

    @Schema(description = "账簿余额方向模式")
    private Integer ledgerBalanceMode;

    @Schema(description = "结账前是否要求凭证审核")
    private Boolean voucherReviewRequired;

}
