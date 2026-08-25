package cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购询价单 Response VO")
@Data
public class ErpPurchaseInquiryRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "询价单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XJD001")
    private String no;

    @Schema(description = "询价主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "原料询价")
    private String inquiryName;

    @Schema(description = "供应商编号列表，逗号分隔", example = "1,2,3")
    private String supplierIds;

    @Schema(description = "供应商名称列表，逗号分隔", example = "供应商A，供应商B")
    private String supplierNames;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "合计金额，单位：元", example = "10000.00")
    private BigDecimal totalAmount;

    @Schema(description = "期望交货日期", example = "2026-08-01")
    private LocalDate expectedDeliveryDate;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建人名称", example = "智云")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "询价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "11756")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品名称", example = "巧克力")
        private String productName;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal quantity;

        @Schema(description = "单位", example = "盒")
        private String unit;

        @Schema(description = "期望价，单位：元", example = "12.50")
        private BigDecimal unitPrice;

        @Schema(description = "期望交货日期", example = "2026-08-01")
        private LocalDate deliveryDate;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
