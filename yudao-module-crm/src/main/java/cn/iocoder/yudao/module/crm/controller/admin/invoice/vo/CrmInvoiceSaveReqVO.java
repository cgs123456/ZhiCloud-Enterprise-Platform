package cn.iocoder.yudao.module.crm.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - CRM 开票创建/更新 Request VO")
@Data
public class CrmInvoiceSaveReqVO {

    @Schema(description = "编号", example = "25787")
    private Long id;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "客户编号", example = "2")
    private Long customerId;

    @Schema(description = "联系人编号", example = "2")
    private Long contactId;

    @Schema(description = "负责人的用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "负责人编号不能为空")
    private Long ownerUserId;

    @Schema(description = "发票类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发票类型不能为空")
    private Integer invoiceType;

    @Schema(description = "发票号码", example = "FP001")
    private String invoiceNo;

    @Schema(description = "购方名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道有限公司")
    @NotNull(message = "购方名称不能为空")
    private String buyerName;

    @Schema(description = "购方税号", example = "91330106MA12345678")
    private String buyerTaxNo;

    @Schema(description = "不含税金额", example = "1000.00")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "含税金额", example = "1130.00")
    private BigDecimal amountWithTax;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-02-02")
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @Schema(description = "发票附件 URL 列表", example = "[\"https://www.iocoder.cn/1.pdf\"]")
    private List<String> fileUrls;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "开票明细列表")
    private List<Line> lines;

    @Schema(description = "开票明细")
    @Data
    public static class Line {

        @Schema(description = "编号", example = "1")
        private Long id;

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道产品")
        @NotNull(message = "产品名称不能为空")
        private String productName;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;

        @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;

        @Schema(description = "不含税金额", example = "1000.00")
        private BigDecimal amountWithoutTax;

        @Schema(description = "税率", example = "13.00")
        private BigDecimal taxRate;

        @Schema(description = "税额", example = "130.00")
        private BigDecimal taxAmount;

        @Schema(description = "含税金额", example = "1130.00")
        private BigDecimal amountWithTax;

        @Schema(description = "备注", example = "备注")
        private String remark;

    }

}
