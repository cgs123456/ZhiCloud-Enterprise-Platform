package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 绩效计划 VO（新增/修改/响应共用）")
@Data
public class HrmPerformancePlanVO {

    @Schema(description = "绩效计划编号")
    private Long id;

    @Schema(description = "计划名称")
    @NotEmpty(message = "计划名称不能为空")
    private String name;

    @Schema(description = "考核周期类型")
    private Integer cycleType;

    @Schema(description = "考核周期")
    private String cycle;

    @Schema(description = "季度")
    private Integer quarter;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "考核说明")
    private String description;

    @Schema(description = "考评范围列表")
    private List<Map<String, Object>> scopes;

    @Schema(description = "考核模板编号")
    private Long assessmentTemplateId;

    @Schema(description = "考核配置快照")
    private Map<String, Object> assessmentConfig;

    @Schema(description = "结果模板编号")
    private Long resultTemplateId;

    @Schema(description = "结果配置快照")
    private Map<String, Object> resultConfig;

    @Schema(description = "指标制定方式")
    private Integer quotaSettingType;

    @Schema(description = "是否开启目标确认")
    private Boolean targetConfirmation;

    @Schema(description = "目标确认节点")
    private Map<String, Object> targetConfirmationStage;

    @Schema(description = "评分阶段列表")
    private List<Map<String, Object>> reviewStages;

    @Schema(description = "是否开启结果审核")
    private Boolean resultAudit;

    @Schema(description = "结果审核节点列表")
    private List<Map<String, Object>> resultAuditStages;

    @Schema(description = "是否开启结果确认")
    private Boolean resultConfirmation;

    @Schema(description = "申诉确认节点列表")
    private List<Map<String, Object>> appealStages;

    @Schema(description = "申诉超期天数")
    private Integer appealTimeoutDays;

    @Schema(description = "申诉超期处理动作")
    private Integer appealTimeoutAction;

    @Schema(description = "是否同步薪资")
    private Boolean syncToSalary;

    @Schema(description = "计薪月份")
    private String paidForMonth;

    @Schema(description = "考核模板名称（enrich）")
    private String assessmentTemplateName;

    @Schema(description = "结果模板名称（enrich）")
    private String resultTemplateName;

    @Schema(description = "当前阶段")
    private Integer stageType;

    @Schema(description = "计划状态")
    private Integer status;

    @Schema(description = "可操作阶段")
    private Integer operationType;

    @Schema(description = "终止时间")
    private LocalDateTime terminateTime;

    @Schema(description = "参评员工数量（enrich）")
    private Long employeeCount;

    @Schema(description = "已完成人数（enrich）")
    private Long finishedCount;

    @Schema(description = "是否可开启评分（enrich）")
    private Boolean scoringReady;

    @Schema(description = "是否可发起面谈（enrich）")
    private Boolean interviewReady;

    @Schema(description = "是否可归档（enrich）")
    private Boolean archiveReady;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
