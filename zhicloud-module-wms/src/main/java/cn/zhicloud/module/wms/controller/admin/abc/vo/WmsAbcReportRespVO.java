package cn.zhicloud.module.wms.controller.admin.abc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WMS ABC 分类报告 VO
 *
 * @author 智云
 */
@Schema(description = "管理后台 - WMS ABC 分类报告 Response VO")
@Data
public class WmsAbcReportRespVO {

    @Schema(description = "统计开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startDate;
    @Schema(description = "统计结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endDate;
    @Schema(description = "分析时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime analysisTime;

    @Schema(description = "总 SKU 数", example = "100")
    private Integer totalSkuCount;
    @Schema(description = "总出库数量", example = "10000.00")
    private BigDecimal totalOutQuantity;
    @Schema(description = "总出库金额", example = "500000.00")
    private BigDecimal totalOutAmount;

    @Schema(description = "A 类 SKU 数", example = "20")
    private Integer classACount;
    @Schema(description = "A 类出库量占比", example = "80.00")
    private BigDecimal classAPercent;
    @Schema(description = "B 类 SKU 数", example = "30")
    private Integer classBCount;
    @Schema(description = "B 类出库量占比", example = "15.00")
    private BigDecimal classBPercent;
    @Schema(description = "C 类 SKU 数", example = "50")
    private Integer classCCount;
    @Schema(description = "C 类出库量占比", example = "5.00")
    private BigDecimal classCPercent;

    @Schema(description = "ABC 分类明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WmsAbcReportItemRespVO> items;

}
