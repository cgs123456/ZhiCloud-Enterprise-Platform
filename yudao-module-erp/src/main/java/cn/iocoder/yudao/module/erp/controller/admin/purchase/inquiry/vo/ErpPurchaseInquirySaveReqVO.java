package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购询价单新增/修改 Request VO")
@Data
public class ErpPurchaseInquirySaveReqVO {

    @Schema(description = "编号", example = "17386")
    private Long id;

    @Schema(description = "询价主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "上半年原料采购询价")
    @NotEmpty(message = "询价主题不能为空")
    private String inquiryName;

    @Schema(description = "供应商编号列表，逗号分隔", requiredMode = Schema.RequiredMode.REQUIRED, example = "1,2,3")
    @NotEmpty(message = "供应商不能为空")
    private String supplierIds;

    @Schema(description = "期望交货日期", example = "2026-08-01")
    private LocalDate expectedDeliveryDate;

    @Schema(description = "备注", example = "请尽快报价")
    private String remark;

    @Schema(description = "询价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "11756")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品名称", example = "巧克力")
        private String productName;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "数量不能为空")
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
