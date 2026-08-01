package cn.iocoder.yudao.module.qms.controller.admin.traceability.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * QMS 质量追溯结果 VO
 *
 * <p>包含正向追溯（原料 → 成品）或反向追溯（成品 → 原料）的追溯链树形结构。
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - QMS 质量追溯结果 Response VO")
@Data
public class QualityTraceabilityRespVO {

    @Schema(description = "追溯方向", example = "FORWARD")
    private String direction;

    @Schema(description = "起始批次号", example = "BATCH20240101001")
    private String startBatchNo;

    @Schema(description = "起始物料/产品 ID", example = "1024")
    private Long startMaterialId;

    @Schema(description = "追溯链根节点列表")
    private List<QualityTraceNodeVO> traceChain = new ArrayList<>();

}
