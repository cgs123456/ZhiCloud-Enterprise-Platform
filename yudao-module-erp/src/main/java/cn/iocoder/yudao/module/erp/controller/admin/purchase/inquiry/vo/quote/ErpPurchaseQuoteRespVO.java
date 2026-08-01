package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.quote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购报价单 Response VO")
@Data
public class ErpPurchaseQuoteRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "报价单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BJD001")
    private String no;

    @Schema(description = "询价单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inquiryId;

    @Schema(description = "询价单号", example = "XJD001")
    private String inquiryNo;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "芋道")
    private String supplierName;

    @Schema(description = "报价时间", example = "2026-07-31 12:00:00")
    private LocalDateTime quoteDate;

    @Schema(description = "合计金额，单位：元", example = "10000.00")
    private BigDecimal totalAmount;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer status;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建人", example = "芋道")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "报价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "11756")
        private Long id;

        @Schema(description = "询价单明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        private Long inquiryItemId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品名称", example = "巧克力")
        private String productName;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal quantity;

        @Schema(description = "报价单价，单位：元", example = "12.50")
        private BigDecimal unitPrice;

        @Schema(description = "报价金额，单位：元", example = "1250.00")
        private BigDecimal amount;

        @Schema(description = "报价交货日期", example = "2026-08-01")
        private LocalDate deliveryDate;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
