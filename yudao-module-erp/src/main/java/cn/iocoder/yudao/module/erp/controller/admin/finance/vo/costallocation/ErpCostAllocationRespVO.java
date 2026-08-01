package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 成本分摊 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCostAllocationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "源成本中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("源成本中心编号")
    private Long costCenterId;

    @Schema(description = "分摊类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("分摊类型")
    private Integer allocationType;

    @Schema(description = "分摊金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("分摊金额")
    private BigDecimal amount;

    @Schema(description = "分摊日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-29")
    @ExcelProperty("分摊日期")
    private LocalDate allocationDate;

    @Schema(description = "目标成本中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("目标成本中心编号")
    private Long targetCostCenterId;

    @Schema(description = "备注", example = "Q3 分摊")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
