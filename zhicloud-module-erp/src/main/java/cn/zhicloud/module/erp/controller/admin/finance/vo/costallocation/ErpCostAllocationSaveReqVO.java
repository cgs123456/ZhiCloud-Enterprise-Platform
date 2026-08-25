package cn.zhicloud.module.erp.controller.admin.finance.vo.costallocation;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.erp.enums.finance.ErpCostAllocationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 成本分摊新增/修改 Request VO")
@Data
public class ErpCostAllocationSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "源成本中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源成本中心编号不能为空")
    private Long costCenterId;

    @Schema(description = "分摊类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "分摊类型不能为空")
    @InEnum(ErpCostAllocationTypeEnum.class)
    private Integer allocationType;

    @Schema(description = "分摊金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "分摊金额不能为空")
    private BigDecimal amount;

    @Schema(description = "分摊日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-29")
    @NotNull(message = "分摊日期不能为空")
    private LocalDate allocationDate;

    @Schema(description = "目标成本中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标成本中心编号不能为空")
    private Long targetCostCenterId;

    @Schema(description = "备注", example = "Q3 分摊")
    private String remark;

}
