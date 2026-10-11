package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HRM 员工绩效考核 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_assessment", autoResultMap = false)
@KeySequence("hr_perf_assessment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceAssessmentDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 绩效计划编号
     */
    private Long planId;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 考核状态
     */
    private Integer status;

    /**
     * 流程状态
     */
    private Integer processStatus;

    /**
     * 当前阶段
     */
    private Integer stageType;

    /**
     * 当前阶段排序
     */
    private Integer stageSort;

    /**
     * 绩效得分
     */
    private BigDecimal score;

    /**
     * 结果等级
     */
    private String resultLevel;

    /**
     * 绩效系数
     */
    private BigDecimal coefficient;

    /**
     * 目标确认人员工编号
     */
    private Long targetConfirmationEmployeeId;

    /**
     * 目标确认结果
     */
    private Integer targetConfirmationResult;

    /**
     * 目标确认意见
     */
    private String targetConfirmationComment;

    /**
     * 目标确认时间
     */
    private LocalDateTime targetConfirmationTime;

    /**
     * 自评说明
     */
    private String selfComment;

    /**
     * 评分说明
     */
    private String reviewerComment;

    /**
     * 结果说明
     */
    private String resultComment;

    /**
     * 申诉状态
     */
    private Integer appealStatus;

    /**
     * 申诉原因
     */
    private String appealReason;

    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;

}
