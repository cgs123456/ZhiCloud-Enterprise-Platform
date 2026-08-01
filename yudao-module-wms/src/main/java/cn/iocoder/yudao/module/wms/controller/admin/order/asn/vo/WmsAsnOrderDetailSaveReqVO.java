package cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - WMS ASN 到货通知单明细保存 Request VO")
@Data
public class WmsAsnOrderDetailSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "SKU 不能为空")
    private Long skuId;

    @Schema(description = "商品名称", example = "红富士苹果")
    @Size(max = 255, message = "商品名称长度不能超过 255 个字符")
    private String productName;

    @Schema(description = "预计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "预计数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "预计数量必须大于 0")
    private BigDecimal expectedQuantity;

    @Schema(description = "单位", example = "箱")
    @Size(max = 32, message = "单位长度不能超过 32 个字符")
    private String unit;

    @Schema(description = "批次号", example = "LOT20260511")
    @Size(max = 64, message = "批次号长度不能超过 64 个字符")
    private String lotNumber;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}
