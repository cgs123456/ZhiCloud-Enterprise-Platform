package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 管理后台 - ERP 账表明细行 Response VO
 *
 * <p>明细账与多栏账共用（多栏账行额外带 {@code columnAmounts} 专栏金额）。
 * 行类型：1 期初行 / 2 明细行 / 3 本期合计行 / 4 本年累计行（前端按 rowType===2 区分明细样式）。
 * 金额式一期口径：无数量列、无辅助核算列（ADR 266 §2.2）。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表明细行 Response VO")
public class ErpGlLedgerDetailRespVO {

    @Schema(description = "行类型：1 期初 / 2 明细 / 3 本期合计 / 4 本年累计", example = "2")
    private Integer rowType;

    @Schema(description = "分录编号（仅明细行）", example = "1024")
    private Long entryId;

    @Schema(description = "科目编号", example = "1001")
    private Long subjectId;

    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称", example = "库存现金")
    private String subjectName;

    @Schema(description = "会计期间（yyyy-MM）", example = "2026-01")
    private String period;

    @Schema(description = "记账日期（仅明细行）", example = "2026-01-31")
    private LocalDate accountDate;

    @Schema(description = "凭证编号（仅明细行）", example = "1024")
    private Long voucherId;

    @Schema(description = "凭证字号（仅明细行）", example = "记-0001")
    private String voucherNumber;

    @Schema(description = "摘要（汇总行为口径文案：期初余额 / 本期合计 / 本年累计）", example = "收现款")
    private String digest;

    @Schema(description = "借方金额", example = "100.0000")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", example = "0.0000")
    private BigDecimal creditAmount;

    @Schema(description = "余额方向：借 / 贷", example = "借")
    private String balanceDirection;

    @Schema(description = "余额绝对值", example = "100.0000")
    private BigDecimal balance;

    @Schema(description = "多栏账专栏金额（键=直接子科目编号，自然方向净额；其余账表恒为 null）",
            example = "{\"100101\":100.0000}")
    private Map<Long, BigDecimal> columnAmounts;

}
