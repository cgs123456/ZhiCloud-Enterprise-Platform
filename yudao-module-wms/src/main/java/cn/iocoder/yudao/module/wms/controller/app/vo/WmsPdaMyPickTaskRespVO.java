package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 App - PDA 我的拣货任务 Response VO")
@Data
public class WmsPdaMyPickTaskRespVO {

    @Schema(description = "任务编号", example = "1024")
    private Long id;

    @Schema(description = "任务编号", example = "PT202605110001")
    private String taskNo;

    @Schema(description = "出库单编号", example = "1024")
    private Long shipmentOrderId;

    @Schema(description = "出库单号", example = "CK202605110001")
    private String shipmentOrderNo;

    @Schema(description = "SKU 编号", example = "2048")
    private Long skuId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "10kg 箱装")
    private String skuName;

    @Schema(description = "商品名称", example = "红富士苹果")
    private String productName;

    @Schema(description = "应拣数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "已拣数量", example = "80.00")
    private BigDecimal pickedQuantity;

    @Schema(description = "库位编号", example = "1024")
    private Long locationId;

    @Schema(description = "拣货顺序", example = "1")
    private Integer pickSequence;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "拣货时间")
    private LocalDateTime pickTime;

}
