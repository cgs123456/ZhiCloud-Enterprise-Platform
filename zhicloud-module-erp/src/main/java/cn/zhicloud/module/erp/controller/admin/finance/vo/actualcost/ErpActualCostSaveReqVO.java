package cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 实际成本新增/修改 Request VO")
@Data
public class ErpActualCostSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "成本期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotBlank(message = "成本期间不能为空")
    private String costPeriod;

    @Schema(description = "成本项目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成本项目 ID 不能为空")
    private Long costItemId;

    @Schema(description = "实际成本总额", example = "1000.00")
    private BigDecimal actualCost;

    @Schema(description = "实际产量/数量", example = "100")
    private BigDecimal actualQuantity;

    @Schema(description = "单位成本", example = "10.00")
    private BigDecimal unitCost;

    @Schema(description = "差异金额", example = "50.00")
    private BigDecimal varianceAmount;

    @Schema(description = "差异率(%)", example = "5.00")
    private BigDecimal varianceRate;

    @Schema(description = "备注", example = "7月实际成本")
    private String remark;

}
