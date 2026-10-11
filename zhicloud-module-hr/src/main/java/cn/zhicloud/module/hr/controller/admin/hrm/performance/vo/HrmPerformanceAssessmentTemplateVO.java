package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 绩效考核模板 VO（新增/修改/响应共用）")
@Data
public class HrmPerformanceAssessmentTemplateVO {

    @Schema(description = "模板编号")
    private Long id;

    @Schema(description = "模板名称")
    @NotEmpty(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板说明")
    private String illustrate;

    @Schema(description = "计分方式")
    private Integer scoreCalculation;

    @Schema(description = "分数上限类型")
    private Integer upperLimitType;

    @Schema(description = "分数上限")
    private BigDecimal upperLimitScore;

    @Schema(description = "考核维度列表（含 quotas 指标数组）")
    private List<Map<String, Object>> dimensions;

    @Schema(description = "维度数量（响应）")
    private Integer dimensionCount;

    @Schema(description = "指标数量（响应）")
    private Integer quotaCount;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
