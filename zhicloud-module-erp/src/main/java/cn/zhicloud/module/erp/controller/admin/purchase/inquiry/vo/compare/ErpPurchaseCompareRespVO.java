package cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.compare;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购比价单 Response VO")
@Data
public class ErpPurchaseCompareRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "比价单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BJB001")
    private String no;

    @Schema(description = "询价单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inquiryId;

    @Schema(description = "询价单号", example = "XJD001")
    private String inquiryNo;

    @Schema(description = "推荐供应商编号", example = "1724")
    private Long recommendSupplierId;

    @Schema(description = "推荐供应商名称", example = "智云")
    private String recommendSupplierName;

    @Schema(description = "推荐理由", example = "报价最低且交期满足")
    private String recommendReason;

    @Schema(description = "报价总数（参与比价的供应商数量）", example = "3")
    private Integer totalQuoteCount;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer status;

    @Schema(description = "备注", example = "推荐最低价供应商")
    private String remark;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "比价明细行列表")
    private List<Line> lines;

    @Data
    public static class Line {

        @Schema(description = "明细编号", example = "11756")
        private Long id;

        @Schema(description = "询价单明细编号", example = "2048")
        private Long inquiryItemId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品名称", example = "巧克力")
        private String productName;

        @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
        private Long supplierId;

        @Schema(description = "供应商名称", example = "智云")
        private String supplierName;

        @Schema(description = "报价单明细编号", example = "3072")
        private Long quoteItemId;

        @Schema(description = "报价单价，单位：元", example = "12.50")
        private BigDecimal unitPrice;

        @Schema(description = "报价金额，单位：元", example = "1250.00")
        private BigDecimal amount;

        @Schema(description = "报价交货日期", example = "2026-08-01")
        private LocalDate deliveryDate;

        @Schema(description = "是否推荐", example = "true")
        private Boolean isRecommended;

    }

}
