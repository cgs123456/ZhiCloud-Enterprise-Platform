package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 管理后台 - ERP 账表查询 Request VO
 *
 * <p>四个账表（明细账 / 总账 / 科目余额表 / 多栏账）共用一套入参，ADR 266 裁定 7：
 * 明细账与多栏账用 {@code subjectId}（按科目子树聚合）；总账与科目余额表用
 * {@code minLevel}/{@code maxLevel} 与 {@code startSubjectId}/{@code endSubjectId}（按科目编码闭区间）。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表查询 Request VO")
public class ErpGlLedgerReqVO {

    @Schema(description = "FMS 账套编号（账表口径经账簿链过滤，不校验账套存在性）", requiredMode = REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    @Positive(message = "账套编号必须为正整数")
    private Long accountSetId;

    @Schema(description = "会计期间起始月（yyyy-MM）", requiredMode = REQUIRED, example = "2026-01")
    @NotEmpty(message = "会计期间起始月不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "会计期间起始月格式必须为 yyyy-MM")
    private String startMonth;

    @Schema(description = "会计期间结束月（yyyy-MM）", requiredMode = REQUIRED, example = "2026-12")
    @NotEmpty(message = "会计期间结束月不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "会计期间结束月格式必须为 yyyy-MM")
    private String endMonth;

    @Schema(description = "科目编号（明细账 / 多栏账口径，按该科目子树聚合）", example = "1001")
    @Positive(message = "科目编号必须为正整数")
    private Long subjectId;

    @Schema(description = "起始科目编号（总账 / 科目余额表口径，按科目编码闭区间过滤）", example = "1001")
    @Positive(message = "起始科目编号必须为正整数")
    private Long startSubjectId;

    @Schema(description = "结束科目编号（总账 / 科目余额表口径，按科目编码闭区间过滤）", example = "6001")
    @Positive(message = "结束科目编号必须为正整数")
    private Long endSubjectId;

    @Schema(description = "科目级次下限（含）", example = "1")
    @Min(value = 1, message = "科目级次下限不能小于 1")
    @Max(value = 8, message = "科目级次下限不能大于 8")
    private Integer minLevel;

    @Schema(description = "科目级次上限（含）", example = "1")
    @Min(value = 1, message = "科目级次上限不能小于 1")
    @Max(value = 8, message = "科目级次上限不能大于 8")
    private Integer maxLevel;

    /**
     * 期间区间校验：起始月不得晚于结束月
     *
     * <p>字符串比较对 yyyy-MM 零填充格式等价于时间比较；两个入参任一为空时交由 @NotEmpty/@Pattern 报错。
     *
     * @return 区间是否合法
     */
    @Schema(description = "期间区间校验（起始月 ≤ 结束月）", hidden = true)
    @AssertTrue(message = "会计期间起始月不能晚于结束月")
    public boolean isPeriodRangeValid() {
        return startMonth == null || endMonth == null || startMonth.compareTo(endMonth) <= 0;
    }

}
