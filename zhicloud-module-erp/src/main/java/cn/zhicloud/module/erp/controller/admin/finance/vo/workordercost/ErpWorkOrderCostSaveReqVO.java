package cn.zhicloud.module.erp.controller.admin.finance.vo.workordercost;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 工单成本归集新增/修改 Request VO")
@Data
public class ErpWorkOrderCostSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单 ID 不能为空")
    private Long workOrderId;

    @Schema(description = "工单编码", example = "WO2026070001")
    private String workOrderCode;

    @Schema(description = "产品 ID", example = "1")
    private Long productId;

    @Schema(description = "成本期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotBlank(message = "成本期间不能为空")
    private String costPeriod;

    @Schema(description = "材料成本", example = "500.00")
    private BigDecimal materialCost;

    @Schema(description = "人工成本", example = "200.00")
    private BigDecimal laborCost;

    @Schema(description = "制造费用", example = "150.00")
    private BigDecimal overheadCost;

    @Schema(description = "外协成本", example = "100.00")
    private BigDecimal outsourcingCost;

    @Schema(description = "总成本", example = "950.00")
    private BigDecimal totalCost;

    @Schema(description = "工单产量", example = "100")
    private BigDecimal quantity;

    @Schema(description = "单位成本", example = "9.50")
    private BigDecimal unitCost;

    @Schema(description = "备注")
    private String remark;

}
