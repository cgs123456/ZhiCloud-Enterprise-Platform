package cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 越库单明细保存 Request VO")
@Data
public class WmsCrossDockOrderDetailSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "SKU 不能为空")
    private Long skuId;

    @Schema(description = "商品名称", example = "红富士苹果")
    @Size(max = 255, message = "商品名称长度不能超过 255 个字符")
    private String productName;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "数量必须大于 0")
    private BigDecimal quantity;

    @Schema(description = "单价", example = "1000.00")
    @DecimalMin(value = "0", message = "单价不能小于 0")
    private BigDecimal unitPrice;

    @Schema(description = "行金额", example = "1500.00")
    @DecimalMin(value = "0", message = "行金额不能小于 0")
    private BigDecimal amount;

    @Schema(description = "关联入库明细编号", example = "3072")
    private Long receiptDetailId;

    @Schema(description = "关联出库明细编号", example = "4096")
    private Long shipmentDetailId;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}
