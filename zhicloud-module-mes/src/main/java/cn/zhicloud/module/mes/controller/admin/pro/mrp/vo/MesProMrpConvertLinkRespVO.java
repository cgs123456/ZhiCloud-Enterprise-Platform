package cn.zhicloud.module.mes.controller.admin.pro.mrp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES MRP 结果行→工单回链 Response VO")
@Data
public class MesProMrpConvertLinkRespVO {

    @Schema(description = "MRP 结果行编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long resultId;

    @Schema(description = "产品物料编号", example = "1")
    private Long productId;

    @Schema(description = "产品物料名称", example = "成品A")
    private String productName;

    @Schema(description = "计划订单量", example = "100.00")
    private BigDecimal plannedOrderQty;

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long workOrderId;

    @Schema(description = "工单编码", example = "MRP-100-1")
    private String workOrderCode;

    @Schema(description = "工单类型（对齐 MesProWorkOrderTypeEnum：1 自行生产 / 2 代工 / 3 采购）", example = "1")
    private Integer workOrderType;

}
