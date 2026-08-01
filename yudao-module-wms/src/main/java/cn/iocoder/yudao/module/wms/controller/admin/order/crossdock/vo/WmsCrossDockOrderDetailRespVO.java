package cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 越库单明细 Response VO")
@Data
public class WmsCrossDockOrderDetailRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "越库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long skuId;

    @Schema(description = "商品名称", example = "红富士苹果")
    private String productName;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "单价", example = "1000.00")
    private BigDecimal unitPrice;

    @Schema(description = "行金额", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "关联入库明细编号", example = "3072")
    private Long receiptDetailId;

    @Schema(description = "关联出库明细编号", example = "4096")
    private Long shipmentDetailId;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
