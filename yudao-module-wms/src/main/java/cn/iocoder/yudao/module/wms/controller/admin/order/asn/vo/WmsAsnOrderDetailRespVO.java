package cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS ASN 到货通知单明细 Response VO")
@Data
public class WmsAsnOrderDetailRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "ASN 单编号", example = "1024")
    private Long asnOrderId;

    @Schema(description = "SKU 编号", example = "2048")
    private Long skuId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "10kg 箱装")
    private String skuName;

    @Schema(description = "商品名称", example = "红富士苹果")
    private String productName;

    @Schema(description = "预计数量", example = "100.00")
    private BigDecimal expectedQuantity;

    @Schema(description = "已收数量", example = "80.00")
    private BigDecimal receivedQuantity;

    @Schema(description = "单位", example = "箱")
    private String unit;

    @Schema(description = "批次号", example = "LOT20260511")
    private String lotNumber;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
