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
 * HRM 绩效考核维度 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_assessment_dimension", autoResultMap = false)
@KeySequence("hr_perf_dimension_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceAssessmentDimensionDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工绩效考核编号
     */
    private Long assessmentId;

    /**
     * 维度名称
     */
    private String name;

    /**
     * 指标类型
     */
    private Integer quotaType;

    /**
     * 维度权重
     */
    private BigDecimal weight;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否允许员工编辑指标
     */
    private Boolean allowEdit;

    /**
     * 排序
     */
    private Integer sort;

}
