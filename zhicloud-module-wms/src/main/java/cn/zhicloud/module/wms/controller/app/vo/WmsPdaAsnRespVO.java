package cn.zhicloud.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 App - PDA ASN 详情 Response VO")
@Data
public class WmsPdaAsnRespVO {

    @Schema(description = "ASN 编号", example = "1024")
    private Long id;

    @Schema(description = "ASN 编号", example = "ASN202605110001")
    private String no;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "月台编号", example = "1024")
    private Long dockId;

    @Schema(description = "月台名称", example = "1号收货月台")
    private String dockName;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "预计到货时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "ASN 明细列表")
    private List<AsnDetail> details;

    @Schema(description = "ASN 明细项")
    @Data
    public static class AsnDetail {

        @Schema(description = "明细编号", example = "1024")
        private Long id;

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

    }

}
