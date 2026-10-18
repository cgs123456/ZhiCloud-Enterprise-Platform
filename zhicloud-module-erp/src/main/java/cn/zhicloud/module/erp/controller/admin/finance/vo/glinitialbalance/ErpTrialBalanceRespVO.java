package cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 期初余额试算平衡 Response VO（C6③ / ADR 265）
 *
 * <p>期初按科目余额方向归集（借向计借方；贷向计贷方；双向按符号拆分）；
 * 累计发生额直接取列。差额 = 借 - 贷；绝对值均 < 0.005 视为平衡。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 期初余额试算平衡 Response VO")
@Data
public class ErpTrialBalanceRespVO {

    @Schema(description = "期初余额借方合计", example = "3000.0000")
    private BigDecimal openingDebitAmount;

    @Schema(description = "期初余额贷方合计", example = "3000.0000")
    private BigDecimal openingCreditAmount;

    @Schema(description = "期初余额差额（借 - 贷）", example = "0.0000")
    private BigDecimal openingDifferenceAmount;

    @Schema(description = "本年累计借方合计", example = "0.0000")
    private BigDecimal yearDebitAmount;

    @Schema(description = "本年累计贷方合计", example = "0.0000")
    private BigDecimal yearCreditAmount;

    @Schema(description = "本年累计差额（借 - 贷）", example = "0.0000")
    private BigDecimal yearDifferenceAmount;

    @Schema(description = "是否试算平衡", example = "true")
    private Boolean balanced;

}
