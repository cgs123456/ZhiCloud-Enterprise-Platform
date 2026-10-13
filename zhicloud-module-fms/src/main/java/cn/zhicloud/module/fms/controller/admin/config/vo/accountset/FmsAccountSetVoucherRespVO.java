package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FMS 账套下的 ERP 凭证流水 Response VO（只读）
 *
 * <p>V193（台账 §12.213）：FMS 账套详情页展示「本账套的凭证流水」。
 *
 * <p>刻意做成独立 VO 而非复用 ERP 的 ErpGlVoucherRespVO：跨模块 VO 泄漏会让 ERP 侧字段
 * 变化直接改到 FMS 的接口契约，边界处独立转换（CLAUDE.md 边界纪律）。只暴露账套视角需要的
 * 字段，不暴露分录明细（避免一次查询拉回大量行）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 账套下的 ERP 凭证流水")
@Data
public class FmsAccountSetVoucherRespVO {

    @Schema(description = "凭证编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "凭证字号", requiredMode = Schema.RequiredMode.REQUIRED, example = "记-001")
    private String voucherNo;

    @Schema(description = "凭证日期", example = "2026-01-31")
    private LocalDate voucherDate;

    @Schema(description = "会计期间", example = "202601")
    private String periodCode;

    @Schema(description = "凭证类型（10 收款 / 20 付款 / 30 转账 / 40 记账）", example = "40")
    private Integer voucherType;

    @Schema(description = "凭证类型名称", example = "记账")
    private String voucherTypeName;

    @Schema(description = "摘要", example = "计提本月折旧")
    private String summary;

    @Schema(description = "借方合计", example = "1000.00")
    private BigDecimal debitTotal;

    @Schema(description = "贷方合计", example = "1000.00")
    private BigDecimal creditTotal;

    @Schema(description = "状态（10 草稿 / 20 已审核）", example = "20")
    private Integer status;

    @Schema(description = "状态名称", example = "已审核")
    private String statusName;

    private String preparedBy;

    private String approvedBy;

    private LocalDateTime approvedTime;
}
