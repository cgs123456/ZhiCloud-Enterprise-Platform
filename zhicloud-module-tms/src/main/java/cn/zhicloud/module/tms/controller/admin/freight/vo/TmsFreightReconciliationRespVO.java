package cn.zhicloud.module.tms.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS 运费对账 Response VO")
@Data
public class TmsFreightReconciliationRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "对账单号", example = "FR001")
    private String no;

    @Schema(description = "承运商编号", example = "1")
    private Long carrierId;

    @Schema(description = "对账周期开始日期", example = "2026-08-01")
    private LocalDate periodStart;

    @Schema(description = "对账周期结束日期", example = "2026-08-31")
    private LocalDate periodEnd;

    @Schema(description = "系统运费总额", example = "15000.00")
    private BigDecimal systemAmount;

    @Schema(description = "承运商账单金额", example = "15200.00")
    private BigDecimal carrierAmount;

    @Schema(description = "差异金额", example = "200.00")
    private BigDecimal diffAmount;

    @Schema(description = "对账状态", example = "0")
    private Integer status;

    @Schema(description = "对账人", example = "1")
    private Long reconcilerId;

    @Schema(description = "对账时间")
    private LocalDateTime reconcileTime;

    @Schema(description = "确认人", example = "1")
    private Long confirmerId;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "备注", example = "差异已核实")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
