package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - ERP 账表总账行 Response VO
 *
 * <p>行类型：1 期初行 / 2 月度合计行 / 3 本年累计行；同一科目的多行连续返回（前端合并编码与名称列）。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表总账行 Response VO")
public class ErpGlLedgerGeneralRespVO {

    @Schema(description = "行类型：1 期初 / 2 月度合计 / 3 本年累计", example = "2")
    private Integer rowType;

    @Schema(description = "科目编号", example = "1001")
    private Long subjectId;

    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称", example = "库存现金")
    private String subjectName;

    @Schema(description = "会计期间（yyyy-MM；期初行为起始月，本年累计行为结束月）", example = "2026-01")
    private String period;

    @Schema(description = "摘要（期初余额 / 本月合计 / 本年累计）", example = "本月合计")
    private String digest;

    @Schema(description = "借方发生额", example = "100.0000")
    private BigDecimal debitAmount;

    @Schema(description = "贷方发生额", example = "0.0000")
    private BigDecimal creditAmount;

    @Schema(description = "余额方向：借 / 贷", example = "借")
    private String balanceDirection;

    @Schema(description = "余额绝对值", example = "100.0000")
    private BigDecimal balance;

}
