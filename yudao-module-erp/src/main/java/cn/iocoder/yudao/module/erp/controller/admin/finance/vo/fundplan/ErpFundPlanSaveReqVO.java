package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fundplan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 资金计划新增/修改 Request VO")
@Data
public class ErpFundPlanSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "计划期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07")
    @NotBlank(message = "计划期间不能为空")
    private String planPeriod;

    @Schema(description = "计划类型（10 收款 20 付款）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "计划类型不能为空")
    private Integer planType;

    @Schema(description = "计划金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50000.00")
    @NotNull(message = "计划金额不能为空")
    private BigDecimal amount;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "备注", example = "7 月回款计划")
    private String remark;

}