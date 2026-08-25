package cn.zhicloud.module.wms.controller.admin.inventory.batch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * WMS 批次出库策略 Response VO
 *
 * <p>FIFO（先进先出）/ FEFO（先到期先出）策略返回的批次出库顺序
 *
 * @author 智云
 */
@Schema(description = "管理后台 - WMS 批次出库策略 Response VO")
@Data
public class WmsInventoryBatchStrategyRespVO {

    @Schema(description = "库存编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inventoryId;

    @Schema(description = "需求数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal demandQuantity;

    @Schema(description = "可分配数量", example = "80.00")
    private BigDecimal allocatedQuantity;

    @Schema(description = "是否充足", example = "true")
    private Boolean sufficient;

    @Schema(description = "策略", requiredMode = Schema.RequiredMode.REQUIRED, example = "FIFO")
    private String strategy;

    @Schema(description = "批次出库顺序列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<BatchAllocation> allocations;

    @Schema(description = "批次出库分配明细")
    @Data
    public static class BatchAllocation {

        @Schema(description = "访问顺序（从 1 开始）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer sequence;

        @Schema(description = "批次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long batchId;

        @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BATCH202605110001")
        private String batchNo;

        @Schema(description = "生产日期")
        private LocalDate productionDate;

        @Schema(description = "过期日期")
        private LocalDate expiryDate;

        @Schema(description = "批次可用数量", example = "100.00")
        private BigDecimal availableQuantity;

        @Schema(description = "本次分配数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
        private BigDecimal allocateQuantity;

    }

}
