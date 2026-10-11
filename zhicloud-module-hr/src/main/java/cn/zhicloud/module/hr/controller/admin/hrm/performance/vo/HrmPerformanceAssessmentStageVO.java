package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 绩效考核阶段 VO")
@Data
public class HrmPerformanceAssessmentStageVO {

    @Schema(description = "考核阶段编号")
    private Long id;

    @Schema(description = "员工绩效考核编号")
    private Long assessmentId;

    @Schema(description = "业务阶段类型")
    private Integer type;

    @Schema(description = "处理员工编号")
    private Long handlerEmployeeId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "阶段名称")
    private String name;

    @Schema(description = "评分人类型")
    private Integer raterType;

    @Schema(description = "阶段权重")
    private BigDecimal weight;

    @Schema(description = "评分方式")
    private Integer scoringType;

    @Schema(description = "可见内容")
    private Integer visibleContent;

    @Schema(description = "评语是否必填")
    private Boolean requiredSetting;

    @Schema(description = "是否允许驳回")
    private Boolean rejectAuthority;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "阶段状态")
    private Integer status;

    @Schema(description = "阶段得分")
    private BigDecimal score;

    @Schema(description = "阶段结果等级")
    private String resultLevel;

    @Schema(description = "评分说明")
    private String comment;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "截止时间")
    private LocalDateTime deadlineTime;

    @Schema(description = "是否可处理")
    private Boolean canHandle;

    @Schema(description = "是否可评分")
    private Boolean canScore;

    @Schema(description = "指标评分列表")
    private List<Map<String, Object>> quotaScoreList;

}
