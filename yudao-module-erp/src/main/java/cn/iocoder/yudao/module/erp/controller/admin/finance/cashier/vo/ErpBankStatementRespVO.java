package cn.iocoder.yudao.module.erp.controller.admin.finance.cashier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行对账单 VO（网银直联返回）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 银行对账单 Response VO")
@Data
public class ErpBankStatementRespVO {

    @Schema(description = "银行流水号", example = "BANK20260701000001")
    private String bankSerialNo;

    @Schema(description = "交易金额", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "交易方向 10收入/20支出", example = "10")
    private Integer direction;

    @Schema(description = "对方名称", example = "客户A")
    private String counterpartyName;

    @Schema(description = "对方账号", example = "622848")
    private String counterpartyAccount;

    @Schema(description = "对方开户行", example = "工商银行")
    private String counterpartyBank;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "摘要", example = "货款")
    private String summary;

}
