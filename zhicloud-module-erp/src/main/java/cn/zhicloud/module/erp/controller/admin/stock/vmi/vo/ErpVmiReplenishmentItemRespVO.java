package cn.zhicloud.module.erp.controller.admin.stock.vmi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP VMI 补货建议明细 Response VO")
@Data
public class ErpVmiReplenishmentItemRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "补货建议编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long replenishmentId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "建议补货数量", example = "50")
    private BigDecimal quantity;

    @Schema(description = "当前库存数量", example = "10")
    private BigDecimal currentQuantity;

    @Schema(description = "系统建议补货数量", example = "60")
    private BigDecimal suggestedQuantity;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
