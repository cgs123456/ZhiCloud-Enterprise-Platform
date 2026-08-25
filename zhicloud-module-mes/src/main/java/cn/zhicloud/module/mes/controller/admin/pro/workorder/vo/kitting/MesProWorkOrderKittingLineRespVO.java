package cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES 工单齐套分析 - BOM 行明细 Resp VO（P0-5）
 *
 * <p>对每条工单 BOM 行展示：物料信息、需求量、库存量、在途量、可用量、缺口量、齐套状态。
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - MES 工单齐套分析 BOM 行明细 Response VO")
@Data
public class MesProWorkOrderKittingLineRespVO {

    @Schema(description = "BOM 行编号")
    private Long id;

    @Schema(description = "工单编号")
    private Long workOrderId;

    @Schema(description = "物料编号")
    private Long itemId;

    @Schema(description = "物料编码")
    private String itemCode;

    @Schema(description = "物料名称")
    private String itemName;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "单位编号（关联 mes_md_unit_measure.id）")
    private Long unitMeasureId;

    @Schema(description = "BOM 单位用量（每单位产品）")
    private BigDecimal bomQuantity;

    @Schema(description = "工单生产数量")
    private BigDecimal workOrderQuantity;

    @Schema(description = "物料总需求量 = BOM 单位用量 × 工单生产数量")
    private BigDecimal requiredQuantity;

    @Schema(description = "当前库存可用量（未冻结）")
    private BigDecimal stockQuantity;

    @Schema(description = "在途数量（待入库的采购入库单行）")
    private BigDecimal inTransitQuantity;

    @Schema(description = "可用量 = 库存 + 在途")
    private BigDecimal availableQuantity;

    @Schema(description = "缺口量 = max(0, 需求量 - 可用量)")
    private BigDecimal shortageQuantity;

    @Schema(description = "齐套状态（1=齐套, 2=部分齐套, 3=短缺）参见 MesProWorkOrderKittingStatusEnum")
    private Integer kittingStatus;

    @Schema(description = "齐套状态名称")
    private String kittingStatusName;

    @Schema(description = "最早在途预计到货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime earliestArrivalDate;

    @Schema(description = "备注")
    private String remark;

}
