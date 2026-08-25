package cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 发票 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpTaxInvoiceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "发票号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    @ExcelProperty("发票号")
    private String invoiceNo;

    @Schema(description = "发票代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000000000")
    @ExcelProperty("发票代码")
    private String invoiceCode;

    @Schema(description = "发票类型", example = "10")
    @ExcelProperty("发票类型")
    private Integer invoiceType;

    @Schema(description = "购方名称", example = "杭州XX公司")
    @ExcelProperty("购方名称")
    private String buyerName;

    @Schema(description = "购方税号", example = "91330100000000000X")
    @ExcelProperty("购方税号")
    private String buyerTaxNo;

    @Schema(description = "销方名称", example = "上海YY公司")
    @ExcelProperty("销方名称")
    private String sellerName;

    @Schema(description = "销方税号", example = "91310000000000000Y")
    @ExcelProperty("销方税号")
    private String sellerTaxNo;

    @Schema(description = "开票日期", example = "2026-07-29")
    @ExcelProperty("开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "不含税金额", example = "1000.00")
    @ExcelProperty("不含税金额")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税额", example = "130.00")
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计", example = "1130.00")
    @ExcelProperty("价税合计")
    private BigDecimal amountWithTax;

    @Schema(description = "状态", example = "20")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "来源单据类型", example = "sale_out")
    @ExcelProperty("来源单据类型")
    private String sourceOrderType;

    @Schema(description = "来源单据 ID", example = "1")
    @ExcelProperty("来源单据 ID")
    private Long sourceOrderId;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
