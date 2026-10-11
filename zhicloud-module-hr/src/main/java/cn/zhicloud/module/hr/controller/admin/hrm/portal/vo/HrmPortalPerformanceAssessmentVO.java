package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentDimensionVO;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentQuotaVO;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentStageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工自助端 - 员工绩效考核 VO
 *
 * 对齐前端 PortalPerformanceAssessmentVO 契约，并补充各阶段任务计数与明细。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 员工绩效考核 VO")
@Data
public class HrmPortalPerformanceAssessmentVO {

    @Schema(description = "员工绩效考核编号")
    private Long id;

    @Schema(description = "绩效计划编号")
    private Long planId;

    @Schema(description = "绩效计划名称")
    private String name;

    @Schema(description = "考核周期类型")
    private Integer cycleType;

    @Schema(description = "考核周期")
    private String cycle;

    @Schema(description = "考核状态")
    private Integer status;

    @Schema(description = "当前阶段")
    private Integer stageType;

    @Schema(description = "绩效得分")
    private BigDecimal score;

    @Schema(description = "单项评分上限")
    private BigDecimal upperLimitScore;

    @Schema(description = "结果等级")
    private String resultLevel;

    @Schema(description = "绩效系数")
    private BigDecimal coefficient;

    @Schema(description = "结果审核状态")
    private Integer resultAuditStatus;

    @Schema(description = "结果审核时间")
    private LocalDateTime resultAuditTime;

    @Schema(description = "结果审核原因")
    private String resultAuditReason;

    @Schema(description = "目标确认人姓名")
    private String targetConfirmationEmployeeName;

    @Schema(description = "目标确认结果：0驳回 1通过")
    private Integer targetConfirmationResult;

    @Schema(description = "目标确认意见")
    private String targetConfirmationComment;

    @Schema(description = "目标确认时间")
    private LocalDateTime targetConfirmationTime;

    @Schema(description = "申诉原因")
    private String appealReason;

    @Schema(description = "申诉附件地址列表")
    private List<String> appealFileUrls;

    @Schema(description = "申诉退回评分阶段编号列表")
    private List<Long> appealReviewStageIds;

    @Schema(description = "申诉提交时间")
    private LocalDateTime appealSubmitTime;

    @Schema(description = "申诉状态：0无 1待处理 2已通过 3已驳回 4已取消")
    private Integer appealStatus;

    @Schema(description = "申诉时间")
    private LocalDateTime appealTime;

    @Schema(description = "申诉说明")
    private String appealComment;

    @Schema(description = "考核开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考核结束时间")
    private LocalDateTime endTime;

    @Schema(description = "归档时间")
    private LocalDateTime archiveTime;

    @Schema(description = "待填写指标数量")
    private Long fillPendingCount;

    @Schema(description = "已填写指标数量")
    private Long fillCompletedCount;

    @Schema(description = "待确认目标数量")
    private Long targetPendingCount;

    @Schema(description = "已确认目标数量")
    private Long targetCompletedCount;

    @Schema(description = "待评分数量")
    private Long reviewPendingCount;

    @Schema(description = "已评分数量")
    private Long reviewCompletedCount;

    @Schema(description = "待审核结果数量")
    private Long resultAuditPendingCount;

    @Schema(description = "已审核结果数量")
    private Long resultAuditCompletedCount;

    @Schema(description = "待确认结果数量")
    private Long resultConfirmationPendingCount;

    @Schema(description = "已确认结果数量")
    private Long resultConfirmationCompletedCount;

    @Schema(description = "已申诉结果数量")
    private Long resultConfirmationAppealedCount;

    @Schema(description = "待处理申诉数量")
    private Long appealPendingCount;

    @Schema(description = "已处理申诉数量")
    private Long appealCompletedCount;

    @Schema(description = "考核维度列表（详情）")
    private List<HrmPerformanceAssessmentDimensionVO> dimensions;

    @Schema(description = "考核指标列表（详情）")
    private List<HrmPerformanceAssessmentQuotaVO> quotas;

    @Schema(description = "评分阶段列表（详情）")
    private List<HrmPerformanceAssessmentStageVO> reviewStages;

    @Schema(description = "运行阶段列表（详情）")
    private List<HrmPerformanceAssessmentStageVO> stages;

    @Schema(description = "当前待处理阶段（详情）")
    private HrmPerformanceAssessmentStageVO currentStage;

    @Schema(description = "当前评分阶段（详情）")
    private HrmPerformanceAssessmentStageVO currentReviewStage;

}
