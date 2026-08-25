package cn.zhicloud.module.erp.controller.admin.finance.cashier.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 出纳单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCashierRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "出纳单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CND20260701000001")
    @ExcelProperty("出纳单号")
    private String no;

    @Schema(description = "出纳类型 10收款/20付款/30内部转账", example = "10")
    @ExcelProperty("出纳类型")
    private Integer cashierType;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "银行账户名称", example = "招商银行基本户")
    @ExcelProperty("银行账户名称")
    private String bankAccountName;

    @Schema(description = "对方名称", example = "供应商A")
    @ExcelProperty("对方名称")
    private String counterpartyName;

    @Schema(description = "对方账号", example = "622848")
    @ExcelProperty("对方账号")
    private String counterpartyAccount;

    @Schema(description = "对方开户行", example = "工商银行")
    @ExcelProperty("对方开户行")
    private String counterpartyBank;

    @Schema(description = "金额", example = "1000.00")
    @ExcelProperty("金额")
    private BigDecimal amount;

    @Schema(description = "支付方式 10现金/20转账/30支票/40网银", example = "40")
    @ExcelProperty("支付方式")
    private Integer paymentMethod;

    @Schema(description = "支付日期")
    @ExcelProperty("支付日期")
    private LocalDate paymentDate;

    @Schema(description = "状态 10待处理/20已提交银行/30已到账/40已退回", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "银行流水号", example = "BANK20260701000001")
    @ExcelProperty("银行流水号")
    private String bankSerialNo;

    @Schema(description = "关联业务单号", example = "XSDD20260701000001")
    @ExcelProperty("关联业务单号")
    private String businessOrderNo;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
