package cn.iocoder.yudao.module.wms.controller.admin.pickpath.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * WMS 拣货路径 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 拣货路径 Response VO")
@Data
public class WmsPickPathRespVO {

    @Schema(description = "来源单据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;
    @Schema(description = "来源单据号", example = "CK202605110001")
    private String orderNo;

    @Schema(description = "仓库编号", example = "2048")
    private Long warehouseId;
    @Schema(description = "仓库名称", example = "成品仓")
    private String warehouseName;

    @Schema(description = "路径策略", example = "NEAREST_NEIGHBOR")
    private String strategy;

    @Schema(description = "总库位数", example = "5")
    private Integer locationCount;
    @Schema(description = "总拣货数量", example = "100.00")
    private BigDecimal totalQuantity;
    @Schema(description = "预估路径长度（伪度量）", example = "32.50")
    private BigDecimal estimatedDistance;

    @Schema(description = "库位访问顺序列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WmsPickPathLocationRespVO> locations;

}
