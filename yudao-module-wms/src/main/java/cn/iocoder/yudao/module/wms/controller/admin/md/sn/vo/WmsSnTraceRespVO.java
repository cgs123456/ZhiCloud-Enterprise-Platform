package cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 序列号追溯 Response VO")
@Data
public class WmsSnTraceRespVO {

    @Schema(description = "序列号", example = "SN20260730001")
    private String sn;

    @Schema(description = "商品编号")
    private Long productId;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "当前状态")
    private String status;

    @Schema(description = "入库单编号（正向追溯）")
    private Long inboundOrderId;

    @Schema(description = "出库单编号（反向追溯）")
    private Long outboundOrderId;

    @Schema(description = "库存批次编号")
    private Long batchId;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "库位编号")
    private Long locationId;

    @Schema(description = "绑定时间")
    private LocalDateTime boundTime;

    @Schema(description = "出库时间")
    private LocalDateTime shippedTime;

}