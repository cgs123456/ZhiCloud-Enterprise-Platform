package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;

/**
 * HRM 绩效考核指标 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_assessment_quota", autoResultMap = false)
@KeySequence("hr_perf_quota_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceAssessmentQuotaDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工绩效考核编号
     */
    private Long assessmentId;

    /**
     * 考核维度编号
     */
    private Long dimensionId;

    /**
     * 是否允许员工编辑指标
     */
    private Boolean allowEdit;

    /**
     * 是否预设指标
     */
    private Boolean preset;

    /**
     * 维度名称
     */
    private String dimensionName;

    /**
     * 指标名称
     */
    private String name;

    /**
     * 指标说明
     */
    private String description;

    /**
     * 评分标准
     */
    private String standard;

    /**
     * 维度权重
     */
    private BigDecimal dimensionWeight;

    /**
     * 指标权重
     */
    private BigDecimal weight;

    /**
     * 评分类型
     */
    private Integer scoreType;

    /**
     * 目标值
     */
    private String targetValue;

    /**
     * 实际值
     */
    private String actualValue;

    /**
     * 自评分
     */
    private BigDecimal selfScore;

    /**
     * 他评分
     */
    private BigDecimal reviewerScore;

    /**
     * 最终分
     */
    private BigDecimal finalScore;

    /**
     * 备注
     */
    private String comment;

    /**
     * 排序
     */
    private Integer sort;

}
