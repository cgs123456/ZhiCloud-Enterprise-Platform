package cn.zhicloud.module.erp.controller.admin.finance.vo.profitability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 获利能力分析新增/修改 Request VO")
@Data
public class ErpProfitabilityAnalysisSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "利润中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "利润中心编号不能为空")
    private Long profitCenterId;

    @Schema(description = "会计期间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会计期间编号不能为空")
    private Long periodId;

    @Schema(description = "收入", example = "100000.00")
    private BigDecimal revenue;

    @Schema(description = "成本", example = "60000.00")
    private BigDecimal cost;

    @Schema(description = "备注", example = "Q3 获利分析")
    private String remark;

}
