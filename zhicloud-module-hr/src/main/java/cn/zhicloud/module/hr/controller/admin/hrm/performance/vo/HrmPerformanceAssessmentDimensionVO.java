package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - HRM 绩效考核维度 VO")
@Data
public class HrmPerformanceAssessmentDimensionVO {

    @Schema(description = "考核维度编号")
    private Long id;

    @Schema(description = "员工绩效考核编号")
    private Long assessmentId;

    @Schema(description = "维度名称")
    private String name;

    @Schema(description = "指标类型")
    private Integer quotaType;

    @Schema(description = "维度权重")
    private BigDecimal weight;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否允许员工编辑指标")
    private Boolean allowEdit;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "考核指标列表")
    private List<HrmPerformanceAssessmentQuotaVO> quotaList;

}
