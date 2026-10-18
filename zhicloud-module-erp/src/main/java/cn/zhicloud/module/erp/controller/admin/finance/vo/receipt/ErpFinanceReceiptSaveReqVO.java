package cn.zhicloud.module.erp.controller.admin.finance.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 收款单新增/修改 Request VO")
@Data
public class ErpFinanceReceiptSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "23752")
    private Long id;

    @Schema(description = "收款时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收款时间不能为空")
    private LocalDateTime receiptTime;

    @Schema(description = "财务人员编号", example = "19690")
    private Long financeUserId;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "29399")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "收款账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "28989")
    @NotNull(message = "收款账户编号不能为空")
    private Long accountId;

    @Schema(description = "优惠金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "11600")
    @NotNull(message = "优惠金额不能为空")
    private BigDecimal discountPrice;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "客户端幂等键。创建收款单时必填；同一租户内同键同载荷返回原单，同键不同载荷拒绝",
            example = "01J8Y6T4K9Q2W3E4R5T6Y7U8I9")
    private String idempotencyKey;

    @Schema(description = "收款项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收款项列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "收款项编号", example = "11756")
        private Long id;

        @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "业务类型不能为空")
        private Integer bizType;

        @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        @NotNull(message = "业务编号不能为空")
        private Long bizId;

        @Schema(description = "业务单号（销售出库/退货明细由服务端回填；CRM 回款明细由集成链路携带）",
                example = "XSCK20261018000001")
        private String bizNo;

        @Schema(description = "已收金额，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
        @NotNull(message = "已收金额不能为空")
        private BigDecimal receiptedPrice;

        @Schema(description = "本次收款，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
        @NotNull(message = "本次收款不能为空")
        private BigDecimal receiptPrice;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}