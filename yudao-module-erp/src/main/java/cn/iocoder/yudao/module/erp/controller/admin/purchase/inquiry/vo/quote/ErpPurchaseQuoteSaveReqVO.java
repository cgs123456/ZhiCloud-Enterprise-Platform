package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.quote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购报价单新增/修改 Request VO")
@Data
public class ErpPurchaseQuoteSaveReqVO {

    @Schema(description = "编号", example = "17386")
    private Long id;

    @Schema(description = "询价单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "询价单编号不能为空")
    private Long inquiryId;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "报价时间", example = "2026-07-31 12:00:00")
    private LocalDateTime quoteDate;

    @Schema(description = "备注", example = "我方报价")
    private String remark;

    @Schema(description = "报价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号", example = "11756")
        private Long id;

        @Schema(description = "询价单明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "询价单明细编号不能为空")
        private Long inquiryItemId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;

        @Schema(description = "报价单价，单位：元", example = "12.50")
        private BigDecimal unitPrice;

        @Schema(description = "报价交货日期", example = "2026-08-01")
        private LocalDate deliveryDate;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
