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
 * HRM 绩效考核阶段 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_assessment_stage", autoResultMap = false)
@KeySequence("hr_perf_stage_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceAssessmentStageDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工绩效考核编号
     */
    private Long assessmentId;

    /**
     * 业务阶段类型
     */
    private Integer type;

    /**
     * 处理员工编号
     */
    private Long handlerEmployeeId;

    /**
     * 阶段名称
     */
    private String name;

    /**
     * 评分人类型
     */
    private Integer raterType;

    /**
     * 阶段权重
     */
    private BigDecimal weight;

    /**
     * 评分方式
     */
    private Integer scoringType;

    /**
     * 可见内容
     */
    private Integer visibleContent;

    /**
     * 评语是否必填
     */
    private Boolean requiredSetting;

    /**
     * 是否允许驳回
     */
    private Boolean rejectAuthority;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 阶段状态
     */
    private Integer status;

    /**
     * 阶段得分
     */
    private BigDecimal score;

    /**
     * 阶段结果等级
     */
    private String resultLevel;

    /**
     * 评分说明
     */
    private String comment;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 截止时间
     */
    private LocalDateTime deadlineTime;

}
