package cn.zhicloud.module.wms.controller.admin.order.wave.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 波次单 Response VO")
@Data
public class WmsWaveOrderRespVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "波次单号")
    private String no;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "波次策略")
    private Integer strategy;

    @Schema(description = "单据日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "拣货员")
    private String picker;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "出库单数")
    private Integer shipmentCount;

    @Schema(description = "SKU 数")
    private Integer skuCount;

    @Schema(description = "总数量")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额")
    private BigDecimal totalPrice;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
