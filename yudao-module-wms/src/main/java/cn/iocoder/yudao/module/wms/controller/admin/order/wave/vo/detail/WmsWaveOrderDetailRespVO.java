package cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.detail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 波次单明细 Response VO")
@Data
public class WmsWaveOrderDetailRespVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "波次单 ID")
    private Long waveOrderId;

    @Schema(description = "出库单 ID")
    private Long shipmentOrderId;

    @Schema(description = "商品规格 ID")
    private Long skuId;

    @Schema(description = "拣货数量")
    private BigDecimal pickQuantity;

    @Schema(description = "已拣数量")
    private BigDecimal pickedQuantity;

    @Schema(description = "备注")
    private String remark;

}
