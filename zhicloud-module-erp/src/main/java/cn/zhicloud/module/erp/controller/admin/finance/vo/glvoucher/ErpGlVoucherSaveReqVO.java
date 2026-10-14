package cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 会计凭证创建/更新 Request VO（P0-7）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计凭证创建/更新 Request VO")
@Data
public class ErpGlVoucherSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "凭证字号（如 记-001）", example = "记-202607-001")
    @NotBlank(message = "凭证字号不能为空")
    private String voucherNo;

    @Schema(description = "凭证日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    @Schema(description = "会计期间编号", example = "1024")
    private Long periodId;

    @Schema(description = "凭证类型（10 收款 / 20 付款 / 30 转账 / 40 记账）", example = "40")
    @NotNull(message = "凭证类型不能为空")
    private Integer voucherType;

    @Schema(description = "附件张数", example = "1")
    private Integer attachmentCount;

    @Schema(description = "凭证摘要", example = "本月销售收款汇总")
    private String summary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "来源会计事件 ID")
    private Long sourceEventId;

    @Schema(description = "来源单据类型", example = "PURCHASE_IN")
    private String sourceDocType;

    @Schema(description = "来源单据 ID", example = "1024")
    private Long sourceDocId;

    @Schema(description = "来源单据编号", example = "PI20260925001")
    private String sourceDocNo;

    /**
     * 账簿编号（可空；空表示默认主账簿）
     *
     * <p>V191（台账 §12.212）给 erp_account_book 挂了 account_set_id 后，「账套 → 账簿 →
     * 凭证」层级已打通，但本 VO 一直没有 accountBookId 字段——会计事件链生成的凭证
     * （createVoucher）无法指定账簿，account_book_id 恒为 NULL，业务链凭证实际不归
     * 任何账套（§12.215 实证：沙箱 22/22 张凭证 account_book_id 为 NULL）。
     *
     * <p>本次补齐该字段：调用方（含会计事件链）显式传账簿；为空时由服务端按会计准则
     * 推断主账簿，仍推断不出才留 NULL（手工凭证允许挂任意账簿）。
     */
    @Schema(description = "账簿编号（可空；空表示默认主账簿）", example = "1024")
    private Long accountBookId;

    @Schema(description = "分录列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证分录不能为空")
    @Valid
    private List<ErpGlVoucherEntryReqVO> entries;

}
