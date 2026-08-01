package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost;

import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpStandardCostStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 标准成本新增/修改 Request VO")
@Data
public class ErpStandardCostSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "产品A")
    private String productName;

    @Schema(description = "成本项目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成本项目 ID 不能为空")
    private Long costItemId;

    @Schema(description = "标准成本", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "标准成本不能为空")
    private BigDecimal standardCost;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-01")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "状态", example = "10")
    @InEnum(ErpStandardCostStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "2026年标准成本")
    private String remark;

}
