package cn.iocoder.yudao.module.wms.controller.admin.abc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS ABC 分类报告行 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS ABC 分类报告行 Response VO")
@Data
public class WmsAbcReportItemRespVO {

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long itemId;
    @Schema(description = "商品编码", example = "ITEM001")
    private String itemCode;
    @Schema(description = "商品名称", example = "红富士苹果")
    private String itemName;
    @Schema(description = "单位", example = "箱")
    private String unit;

    @Schema(description = "出库频次", example = "120")
    private Long outCount;
    @Schema(description = "出库数量", example = "1000.00")
    private BigDecimal outQuantity;
    @Schema(description = "出库金额", example = "50000.00")
    private BigDecimal outAmount;

    @Schema(description = "出库量占比", example = "12.50")
    private BigDecimal quantityPercent;
    @Schema(description = "累计出库量占比", example = "80.00")
    private BigDecimal cumulativePercent;

    @Schema(description = "ABC 分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "A")
    private String abcClassification;

    @Schema(description = "分析时间")
    private LocalDateTime analysisTime;

}
