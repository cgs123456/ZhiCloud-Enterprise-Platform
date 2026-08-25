package cn.zhicloud.module.erp.controller.admin.finance.cashier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 出纳单新增/修改 Request VO")
@Data
public class ErpCashierSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "出纳单号", example = "CND20260701000001")
    private String no;

    @Schema(description = "出纳类型 10收款/20付款/30内部转账", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "出纳类型不能为空")
    private Integer cashierType;

    @Schema(description = "银行账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "银行账户编号不能为空")
    private Long bankAccountId;

    @Schema(description = "对方名称", example = "供应商A")
    private String counterpartyName;

    @Schema(description = "对方账号", example = "622848")
    private String counterpartyAccount;

    @Schema(description = "对方开户行", example = "工商银行")
    private String counterpartyBank;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "支付方式 10现金/20转账/30支票/40网银", example = "40")
    private Integer paymentMethod;

    @Schema(description = "支付日期")
    private LocalDate paymentDate;

    @Schema(description = "状态 10待处理/20已提交银行/30已到账/40已退回", example = "10")
    private Integer status;

    @Schema(description = "关联业务单号", example = "XSDD20260701000001")
    private String businessOrderNo;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
