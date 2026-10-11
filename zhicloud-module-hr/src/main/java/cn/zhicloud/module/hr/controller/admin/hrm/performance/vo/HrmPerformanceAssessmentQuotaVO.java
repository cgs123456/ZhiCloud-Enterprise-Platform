package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 绩效考核指标 VO")
@Data
public class HrmPerformanceAssessmentQuotaVO {

    @Schema(description = "考核指标编号")
    private Long id;

    @Schema(description = "员工绩效考核编号")
    private Long assessmentId;

    @Schema(description = "考核维度编号")
    private Long dimensionId;

    @Schema(description = "是否允许员工编辑指标")
    private Boolean allowEdit;

    @Schema(description = "是否预设指标")
    private Boolean preset;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "指标名称")
    private String name;

    @Schema(description = "指标说明")
    private String description;

    @Schema(description = "评分标准")
    private String standard;

    @Schema(description = "维度权重")
    private BigDecimal dimensionWeight;

    @Schema(description = "指标权重")
    private BigDecimal weight;

    @Schema(description = "评分类型")
    private Integer scoreType;

    @Schema(description = "目标值")
    private String targetValue;

    @Schema(description = "实际值")
    private String actualValue;

    @Schema(description = "自评分")
    private BigDecimal selfScore;

    @Schema(description = "他评分")
    private BigDecimal reviewerScore;

    @Schema(description = "最终分")
    private BigDecimal finalScore;

    @Schema(description = "备注")
    private String comment;

    @Schema(description = "排序")
    private Integer sort;

}
