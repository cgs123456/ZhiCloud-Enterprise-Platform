package cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 工单齐套分析 - 工单整体汇总 Resp VO（P0-5）
 *
 * <p>对整个工单展示：工单基础信息、整体齐套率、齐套/短缺行数统计、所有 BOM 行明细列表。
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - MES 工单齐套分析汇总 Response VO")
@Data
public class MesProWorkOrderKittingSummaryRespVO {

    @Schema(description = "工单编号")
    private Long workOrderId;

    @Schema(description = "工单编码")
    private String workOrderCode;

    @Schema(description = "工单名称")
    private String workOrderName;

    @Schema(description = "产品编号")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "工单生产数量")
    private BigDecimal workOrderQuantity;

    @Schema(description = "工单状态")
    private Integer workOrderStatus;

    @Schema(description = "需求日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;

    @Schema(description = "BOM 行总数")
    private Integer totalLineCount;

    @Schema(description = "齐套行数（status=1）")
    private Integer fullyKittedCount;

    @Schema(description = "部分齐套行数（status=2）")
    private Integer partialCount;

    @Schema(description = "短缺行数（status=3）")
    private Integer shortageCount;

    @Schema(description = "齐套率（百分比，0-100，保留 2 位小数）")
    private BigDecimal kittingRate;

    @Schema(description = "是否整体齐套（true=所有 BOM 行都齐套）")
    private Boolean fullyKitted;

    @Schema(description = "BOM 行明细列表")
    private List<MesProWorkOrderKittingLineRespVO> lines;

}
