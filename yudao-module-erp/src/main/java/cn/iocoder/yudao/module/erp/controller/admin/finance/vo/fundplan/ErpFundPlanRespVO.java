package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fundplan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资金计划 Response VO")
@Data
public class ErpFundPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "计划期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07")
    private String planPeriod;

    @Schema(description = "计划类型（10 收款 20 付款）", example = "10")
    private Integer planType;

    @Schema(description = "计划金额", example = "50000.00")
    private BigDecimal amount;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "备注", example = "7 月回款计划")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}