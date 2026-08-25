package cn.zhicloud.module.erp.controller.admin.sale.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 客户信用额度新增/修改 Request VO")
@Data
public class ErpCreditLimitSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "信用额度", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    @NotNull(message = "信用额度不能为空")
    private BigDecimal creditLimit;

    @Schema(description = "已用额度", example = "20000.00")
    private BigDecimal usedAmount;

    @Schema(description = "逾期金额", example = "0.00")
    private BigDecimal overdueAmount;

    @Schema(description = "预警比例（默认 80）", example = "80")
    private BigDecimal warningRatio;

    @Schema(description = "状态 10正常/20预警/30冻结", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
