package cn.zhicloud.module.erp.controller.admin.finance.vo.cashflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 现金流记录新增/修改 Request VO")
@Data
public class ErpCashFlowSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "业务类型（10 收款 20 付款）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "业务单据编号", example = "1024")
    private Long bizOrderId;

    @Schema(description = "业务单据类型", example = "SALE_OUT")
    private String bizOrderType;

    @Schema(description = "发生日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-30")
    @NotNull(message = "发生日期不能为空")
    private LocalDate occurDate;

    @Schema(description = "备注", example = "销售出库收款")
    private String remark;

}