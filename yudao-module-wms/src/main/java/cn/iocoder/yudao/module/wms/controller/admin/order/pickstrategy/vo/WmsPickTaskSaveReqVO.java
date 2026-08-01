package cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 拣货任务保存 Request VO")
@Data
public class WmsPickTaskSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "任务编号", example = "PT202605110001")
    @Size(max = 64, message = "任务编号长度不能超过 64 个字符")
    private String taskNo;

    @Schema(description = "出库单编号", example = "1024")
    private Long shipmentOrderId;

    @Schema(description = "波次单编号", example = "1024")
    private Long waveOrderId;

    @Schema(description = "SKU 编号", example = "2048")
    private Long skuId;

    @Schema(description = "商品名称", example = "红富士苹果")
    @Size(max = 255, message = "商品名称长度不能超过 255 个字符")
    private String productName;

    @Schema(description = "应拣数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "库位编号", example = "1024")
    private Long locationId;

    @Schema(description = "拣货顺序", example = "1")
    private Integer pickSequence;

    @Schema(description = "拣货员用户编号", example = "1")
    private Long pickerUserId;

    @Schema(description = "拣货时间")
    private LocalDateTime pickTime;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
