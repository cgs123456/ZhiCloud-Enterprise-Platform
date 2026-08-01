package cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 销售订单创建/更新 Request VO")
@Data
public class CrmSaleOrderSaveReqVO {

    @Schema(description = "编号", example = "10430")
    private Long id;

    @Schema(description = "关联合同编号", example = "10864")
    private Long contractId;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "商机编号", example = "10864")
    private Long businessId;

    @Schema(description = "联系人编号", example = "18546")
    private Long contactId;

    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @NotNull(message = "下单日期不能为空")
    private LocalDateTime orderDate;

    @Schema(description = "交货日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime deliveryDate;

    @Schema(description = "折扣金额", example = "100.00")
    private BigDecimal discountAmount;

    @Schema(description = "负责人的用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17144")
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "订单明细列表")
    private List<Item> items;

    @Schema(description = "订单明细")
    @Data
    public static class Item {

        @Schema(description = "编号", example = "888")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20529")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品名称", example = "iPhone")
        private String productName;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;

        @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "999.00")
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;

        @Schema(description = "折扣", example = "50.00")
        private BigDecimal discount;

        @Schema(description = "税率", example = "0.13")
        private BigDecimal taxRate;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
