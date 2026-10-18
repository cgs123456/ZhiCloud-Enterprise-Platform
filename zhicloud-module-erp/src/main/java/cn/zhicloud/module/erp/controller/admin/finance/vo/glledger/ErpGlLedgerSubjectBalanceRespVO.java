package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - ERP 账表科目余额行 Response VO（树）
 *
 * <p>{@code nodeType} 恒为 1（科目）——金额式一期无辅助核算组合节点，字段保留以维持页面契约
 * （ADR 266 §2.2 第 2 条）。金额按符号拆借贷两列：借正贷负净额 ≥ 0 计入借方列，否则计入贷方列。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表科目余额行 Response VO")
public class ErpGlLedgerSubjectBalanceRespVO {

    @Schema(description = "树节点键（row:科目编号）", example = "row:1001")
    private String nodeKey;

    @Schema(description = "节点类型：1 科目（金额式一期无辅助组合节点）", example = "1")
    private Integer nodeType;

    @Schema(description = "科目编号", example = "1001")
    private Long subjectId;

    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称", example = "库存现金")
    private String subjectName;

    @Schema(description = "科目级次", example = "1")
    private Integer level;

    @Schema(description = "期初借方余额", example = "100.0000")
    private BigDecimal openingDebitAmount;

    @Schema(description = "期初贷方余额", example = "0.0000")
    private BigDecimal openingCreditAmount;

    @Schema(description = "本期借方发生额", example = "50.0000")
    private BigDecimal periodDebitAmount;

    @Schema(description = "本期贷方发生额", example = "0.0000")
    private BigDecimal periodCreditAmount;

    @Schema(description = "本年累计借方发生额", example = "150.0000")
    private BigDecimal yearDebitAmount;

    @Schema(description = "本年累计贷方发生额", example = "0.0000")
    private BigDecimal yearCreditAmount;

    @Schema(description = "期末借方余额", example = "150.0000")
    private BigDecimal endingDebitAmount;

    @Schema(description = "期末贷方余额", example = "0.0000")
    private BigDecimal endingCreditAmount;

    @Schema(description = "子科目余额行", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<ErpGlLedgerSubjectBalanceRespVO> children;

}
