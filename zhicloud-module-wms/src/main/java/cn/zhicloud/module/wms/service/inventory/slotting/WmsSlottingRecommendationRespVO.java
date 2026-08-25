package cn.zhicloud.module.wms.service.inventory.slotting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * WMS 上架推荐结果 VO
 *
 * <p>由 {@link WmsSlottingService#recommendPutaway} 返回，描述上架目标推荐：
 * <ul>
 *   <li>推荐仓库：优先与已有同 SKU 批次合并</li>
 *   <li>推荐库存行：若目标仓库已有该 SKU 的库存，则返回库存 ID；为空表示新建</li>
 *   <li>推荐批次：若存在相同 batchNo 的批次，则返回批次 ID；为空表示新建批次</li>
 * </ul>
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - WMS 上架推荐结果 Response VO")
public class WmsSlottingRecommendationRespVO {

    /**
     * 推荐理由：CONSOLIDATE_BATCH（合并到已有批次）
     */
    public static final String REASON_CONSOLIDATE_BATCH = "CONSOLIDATE_BATCH";
    /**
     * 推荐理由：NEW_BATCH_TO_EXISTING_INVENTORY（已有库存行，新建批次）
     */
    public static final String REASON_NEW_BATCH_TO_EXISTING_INVENTORY = "NEW_BATCH_TO_EXISTING_INVENTORY";
    /**
     * 推荐理由：NEW_INVENTORY（无库存行，新建库存+批次）
     */
    public static final String REASON_NEW_INVENTORY = "NEW_INVENTORY";

    @Schema(description = "推荐上架仓库编号")
    private Long recommendedWarehouseId;

    @Schema(description = "推荐仓库编码")
    private String recommendedWarehouseCode;

    @Schema(description = "推荐仓库名称")
    private String recommendedWarehouseName;

    @Schema(description = "推荐合并的库存行编号，为空表示新建库存行", example = "1024")
    private Long recommendedInventoryId;

    @Schema(description = "推荐合并的批次编号，为空表示新建批次", example = "2048")
    private Long recommendedBatchId;

    @Schema(description = "推荐理由")
    private String reason;

    @Schema(description = "推荐理由说明（中文）")
    private String reasonText;

    @Schema(description = "已有批次的过期日期（合并时用于校验）")
    private LocalDate existingBatchExpiryDate;

}
