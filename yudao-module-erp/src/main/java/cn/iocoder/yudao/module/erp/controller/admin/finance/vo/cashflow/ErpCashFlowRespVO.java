package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 现金流记录 Response VO")
@Data
public class ErpCashFlowRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "业务类型（10 收款 20 付款）", example = "10")
    private Integer bizType;

    @Schema(description = "金额", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "业务单据编号", example = "1024")
    private Long bizOrderId;

    @Schema(description = "业务单据类型", example = "SALE_OUT")
    private String bizOrderType;

    @Schema(description = "发生日期", example = "2026-07-30")
    private LocalDate occurDate;

    @Schema(description = "备注", example = "销售出库收款")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}