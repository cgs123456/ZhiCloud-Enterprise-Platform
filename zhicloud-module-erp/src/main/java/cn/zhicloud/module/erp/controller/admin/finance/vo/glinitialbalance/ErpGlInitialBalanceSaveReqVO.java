package cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 期初余额保存 Request VO（账套口径，C6③ / ADR 265）
 *
 * <p>保存语义 = 差分 upsert：请求内的科目行全量写入（零值行保留），
 * 未出现在请求中的科目行不动。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 期初余额保存 Request VO（账套口径）")
@Data
public class ErpGlInitialBalanceSaveReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "科目期初余额列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期初余额列表不能为空")
    @Valid
    private List<Balance> balances;

    /**
     * 科目期初余额行
     */
    @Schema(description = "科目期初余额行")
    @Data
    public static class Balance {

        @Schema(description = "科目编号（末级科目）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "科目编号不能为空")
        private Long subjectId;

        @Schema(description = "期初余额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
        @NotNull(message = "期初余额不能为空")
        @DecimalMin(value = "0", message = "期初余额不能为负数")
        private BigDecimal openingAmount;

        @Schema(description = "本年累计借方发生额", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.00")
        @NotNull(message = "本年累计借方发生额不能为空")
        @DecimalMin(value = "0", message = "本年累计借方发生额不能为负数")
        private BigDecimal yearDebitAmount;

        @Schema(description = "本年累计贷方发生额", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.00")
        @NotNull(message = "本年累计贷方发生额不能为空")
        @DecimalMin(value = "0", message = "本年累计贷方发生额不能为负数")
        private BigDecimal yearCreditAmount;

        @Schema(description = "实际损益发生额（损益类科目）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.00")
        @NotNull(message = "实际损益发生额不能为空")
        @DecimalMin(value = "0", message = "实际损益发生额不能为负数")
        private BigDecimal profitLossAmount;

    }

}
