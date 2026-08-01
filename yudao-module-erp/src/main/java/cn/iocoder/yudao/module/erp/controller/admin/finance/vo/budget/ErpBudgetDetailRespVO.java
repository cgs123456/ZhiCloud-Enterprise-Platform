package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 预算明细 Response VO（P0-14）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 预算明细 Response VO")
@Data
public class ErpBudgetDetailRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "预算主表编号", example = "1024")
    private Long budgetId;

    @Schema(description = "会计科目编号", example = "10")
    private Long accountId;

    @Schema(description = "科目编码", example = "6602")
    private String accountCode;

    @Schema(description = "科目名称", example = "管理费用")
    private String accountName;

    @Schema(description = "预算金额", example = "100000.00")
    private BigDecimal budgetAmount;

    @Schema(description = "实际金额", example = "95000.00")
    private BigDecimal actualAmount;

    @Schema(description = "差异金额", example = "-5000.00")
    private BigDecimal varianceAmount;

    @Schema(description = "差异率", example = "-0.05")
    private BigDecimal varianceRate;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
