package cn.zhicloud.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 App - PDA 待收货列表 Response VO")
@Data
public class WmsPdaPendingReceiptRespVO {

    @Schema(description = "ASN 编号", example = "1024")
    private Long id;

    @Schema(description = "ASN 编号", example = "ASN202605110001")
    private String no;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "月台名称", example = "1号收货月台")
    private String dockName;

    @Schema(description = "状态", example = "20")
    private Integer status;

    @Schema(description = "预计到货时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "总数量", example = "100.00")
    private BigDecimal totalQuantity;

}
