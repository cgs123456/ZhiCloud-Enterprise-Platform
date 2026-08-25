package cn.zhicloud.module.qms.controller.admin.traceability.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QMS 质量追溯节点 VO
 *
 * <p>用于构建追溯链的树形结构，每个节点代表追溯链中的一个环节（原料批次、生产工单、成品批次、出库记录等）。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - QMS 质量追溯节点 VO")
@Data
public class QualityTraceNodeVO {

    @Schema(description = "节点类型", example = "MATERIAL")
    private String nodeType;

    @Schema(description = "节点 ID", example = "1024")
    private Long id;

    @Schema(description = "批次号", example = "BATCH20240101001")
    private String batchNo;

    @Schema(description = "物料/产品 ID", example = "1024")
    private Long materialId;

    @Schema(description = "物料/产品名称", example = "电阻 10K")
    private String materialName;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "工单 ID", example = "3072")
    private Long workOrderId;

    @Schema(description = "检验单 ID", example = "4096")
    private Long inspectionOrderId;

    @Schema(description = "检验结果", example = "30")
    private Integer inspectionResult;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子节点列表")
    private List<QualityTraceNodeVO> children = new ArrayList<>();

}
