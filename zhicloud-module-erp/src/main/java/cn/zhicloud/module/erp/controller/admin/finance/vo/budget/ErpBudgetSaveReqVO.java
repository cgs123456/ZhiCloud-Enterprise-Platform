package cn.zhicloud.module.erp.controller.admin.finance.vo.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 预算新增/修改 Request VO")
@Data
public class ErpBudgetSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "预算编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BUD-2026-001")
    @NotEmpty(message = "预算编号不能为空")
    private String budgetNo;

    @Schema(description = "预算年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "预算年度不能为空")
    private Integer budgetYear;

    @Schema(description = "期间编号（空表示年度预算）", example = "1")
    private Long periodId;

    @Schema(description = "期间编码", example = "2026")
    private String periodCode;

    @Schema(description = "部门编号", example = "1")
    private Long departmentId;

    @Schema(description = "预算类型（10=运营/20=资本/30=现金流）", example = "10")
    private Integer budgetType;

    @Schema(description = "预算明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预算明细不能为空")
    private List<Detail> details;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "预算明细")
    @Data
    public static class Detail {

        @Schema(description = "明细编号（修改时传入）", example = "1")
        private Long id;

        @Schema(description = "会计科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "会计科目不能为空")
        private Long accountId;

        @Schema(description = "预算金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
        @NotNull(message = "预算金额不能为空")
        private BigDecimal budgetAmount;

        @Schema(description = "排序", example = "1")
        private Integer sort;

        @Schema(description = "备注", example = "备注")
        private String remark;
    }

}
