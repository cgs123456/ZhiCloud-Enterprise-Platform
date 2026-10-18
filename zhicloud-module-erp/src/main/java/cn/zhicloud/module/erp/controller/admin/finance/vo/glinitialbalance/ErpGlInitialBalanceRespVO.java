package cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 期初余额 Response VO（账套口径，C6③ / ADR 265）
 *
 * <p>一行 = 一个科目（科目字段取自 erp_gl_account）+ 四个金额（未录行为 0）；
 * yearOpeningAmount 为派生只读（借向 = opening - yearDebit + yearCredit；
 * 其余方向 = opening + yearDebit - yearCredit）。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 期初余额 Response VO（账套口径）")
@Data
public class ErpGlInitialBalanceRespVO {

    @Schema(description = "科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long subjectId;

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    private String subjectName;

    @Schema(description = "父级科目编号", example = "0")
    private Long parentId;

    @Schema(description = "科目类型", example = "10")
    private Integer type;

    @Schema(description = "科目类型名称", example = "资产")
    private String typeName;

    @Schema(description = "余额方向", example = "10")
    private Integer balanceDirection;

    @Schema(description = "余额方向名称", example = "借方")
    private String balanceDirectionName;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

    @Schema(description = "期初余额", example = "1000.0000")
    private BigDecimal openingAmount;

    @Schema(description = "本年累计借方发生额", example = "0.0000")
    private BigDecimal yearDebitAmount;

    @Schema(description = "本年累计贷方发生额", example = "0.0000")
    private BigDecimal yearCreditAmount;

    @Schema(description = "年初余额（派生只读）", example = "1000.0000")
    private BigDecimal yearOpeningAmount;

    @Schema(description = "实际损益发生额（损益类科目）", example = "0.0000")
    private BigDecimal profitLossAmount;

}
