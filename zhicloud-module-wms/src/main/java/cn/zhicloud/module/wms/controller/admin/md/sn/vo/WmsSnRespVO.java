package cn.zhicloud.module.wms.controller.admin.md.sn.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 序列号 Response VO")
@Data
public class WmsSnRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "序列号", example = "SN20260730001")
    private String sn;

    @Schema(description = "商品编号", example = "1")
    private Long productId;

    @Schema(description = "商品编码", example = "SKU001")
    private String productCode;

    @Schema(description = "商品名称", example = "商品A")
    private String productName;

    @Schema(description = "库存批次编号", example = "10")
    private Long batchId;

    @Schema(description = "库存编号", example = "100")
    private Long inventoryId;

    @Schema(description = "状态", example = "IN_STOCK")
    private String status;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库区编号", example = "1")
    private Long zoneId;

    @Schema(description = "库位编号", example = "1")
    private Long locationId;

    @Schema(description = "入库单编号")
    private Long inboundOrderId;

    @Schema(description = "出库单编号")
    private Long outboundOrderId;

    @Schema(description = "绑定时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime boundTime;

    @Schema(description = "出库时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shippedTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}