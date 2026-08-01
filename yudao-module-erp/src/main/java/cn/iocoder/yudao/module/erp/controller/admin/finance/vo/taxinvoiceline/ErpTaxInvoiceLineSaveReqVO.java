package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 发票明细新增/修改 Request VO")
@Data
public class ErpTaxInvoiceLineSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "发票 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发票 ID 不能为空")
    private Long invoiceId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "物料 A")
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @Schema(description = "规格", example = "10kg")
    private String specification;

    @Schema(description = "单位", example = "kg")
    private String unit;

    @Schema(description = "数量", example = "100")
    private BigDecimal quantity;

    @Schema(description = "单价", example = "10.00")
    private BigDecimal unitPrice;

    @Schema(description = "不含税金额", example = "1000.00")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税率", example = "0.13")
    private BigDecimal taxRate;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计", example = "1130.00")
    private BigDecimal amountWithTax;

    @Schema(description = "备注")
    private String remark;

}
