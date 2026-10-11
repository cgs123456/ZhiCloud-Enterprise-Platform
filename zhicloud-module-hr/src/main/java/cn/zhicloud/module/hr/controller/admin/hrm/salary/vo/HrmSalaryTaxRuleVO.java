package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 计税规则 VO（新增/修改/响应共用）")
@Data
public class HrmSalaryTaxRuleVO {

    @Schema(description = "计税规则编号")
    private Long id;

    @Schema(description = "计税规则名称")
    @NotEmpty(message = "计税规则名称不能为空")
    private String name;

    @Schema(description = "计税类型")
    private Integer type;

    @Schema(description = "是否计税")
    private Boolean taxEnabled;

    @Schema(description = "起征阈值")
    private BigDecimal threshold;

    @Schema(description = "小数位数")
    private Integer decimalScale;

    @Schema(description = "计税周期类型")
    private Integer cycleType;

    @Schema(description = "使用该规则的薪资组数量（enrich）")
    private Long usedGroupCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
