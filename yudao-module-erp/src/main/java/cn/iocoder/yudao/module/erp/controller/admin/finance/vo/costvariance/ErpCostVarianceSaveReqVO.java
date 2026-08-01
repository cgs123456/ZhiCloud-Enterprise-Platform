package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpVarianceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 成本差异新增/修改 Request VO")
@Data
public class ErpCostVarianceSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "成本期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotBlank(message = "成本期间不能为空")
    private String costPeriod;

    @Schema(description = "成本项目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成本项目 ID 不能为空")
    private Long costItemId;

    @Schema(description = "标准成本", example = "100.00")
    private BigDecimal standardCost;

    @Schema(description = "实际成本", example = "110.00")
    private BigDecimal actualCost;

    @Schema(description = "差异金额", example = "10.00")
    private BigDecimal varianceAmount;

    @Schema(description = "差异率(%)", example = "10.00")
    private BigDecimal varianceRate;

    @Schema(description = "差异类型", example = "20")
    @InEnum(ErpVarianceTypeEnum.class)
    private Integer varianceType;

    @Schema(description = "分析说明", example = "材料价格上涨导致不利差异")
    private String analysisRemark;

    @Schema(description = "备注")
    private String remark;

}
