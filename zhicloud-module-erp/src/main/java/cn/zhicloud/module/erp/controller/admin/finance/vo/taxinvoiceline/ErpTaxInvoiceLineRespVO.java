package cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoiceline;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 发票明细 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpTaxInvoiceLineRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "发票 ID", example = "1")
    @ExcelProperty("发票 ID")
    private Long invoiceId;

    @Schema(description = "行号", example = "1")
    @ExcelProperty("行号")
    private Integer lineNo;

    @Schema(description = "商品名称", example = "物料 A")
    @ExcelProperty("商品名称")
    private String productName;

    @Schema(description = "规格", example = "10kg")
    @ExcelProperty("规格")
    private String specification;

    @Schema(description = "单位", example = "kg")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "数量", example = "100")
    @ExcelProperty("数量")
    private BigDecimal quantity;

    @Schema(description = "单价", example = "10.00")
    @ExcelProperty("单价")
    private BigDecimal unitPrice;

    @Schema(description = "不含税金额", example = "1000.00")
    @ExcelProperty("不含税金额")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税率", example = "0.13")
    @ExcelProperty("税率")
    private BigDecimal taxRate;

    @Schema(description = "税额", example = "130.00")
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计", example = "1130.00")
    @ExcelProperty("价税合计")
    private BigDecimal amountWithTax;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
