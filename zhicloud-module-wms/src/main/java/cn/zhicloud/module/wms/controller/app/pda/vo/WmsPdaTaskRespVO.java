package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PDA 任务响应 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 任务 Response VO")
@Data
public class WmsPdaTaskRespVO {

    /**
     * 任务类型：拣货 / 上架 / 盘点
     */
    public static final String TYPE_PICK = "PICK";
    public static final String TYPE_PUTAWAY = "PUTAWAY";
    public static final String TYPE_CHECK = "CHECK";

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PICK")
    private String taskType;

    @Schema(description = "来源单据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;
    @Schema(description = "来源单据号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CK202605110001")
    private String orderNo;

    @Schema(description = "仓库编号", example = "2048")
    private Long warehouseId;
    @Schema(description = "仓库名称", example = "成品仓")
    private String warehouseName;

    @Schema(description = "商品 SKU 编号", example = "4096")
    private Long skuId;
    @Schema(description = "规格编号", example = "SKU001")
    private String skuCode;
    @Schema(description = "规格名称", example = "10kg 箱装")
    private String skuName;
    @Schema(description = "商品编号", example = "8192")
    private Long itemId;
    @Schema(description = "商品编码", example = "ITEM001")
    private String itemCode;
    @Schema(description = "商品名称", example = "红富士苹果")
    private String itemName;
    @Schema(description = "单位", example = "箱")
    private String unit;

    @Schema(description = "需求数量", example = "10.00")
    private BigDecimal demandQuantity;
    @Schema(description = "已执行数量", example = "0.00")
    private BigDecimal executedQuantity;

    @Schema(description = "单据日期")
    private LocalDateTime orderTime;

}
