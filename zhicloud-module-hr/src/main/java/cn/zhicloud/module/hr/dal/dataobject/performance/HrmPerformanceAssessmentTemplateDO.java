package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * HRM 绩效考核模板 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_assessment_template", autoResultMap = true)
@KeySequence("hr_perf_assess_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceAssessmentTemplateDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板说明
     */
    private String illustrate;

    /**
     * 计分方式
     */
    private Integer scoreCalculation;

    /**
     * 分数上限类型
     */
    private Integer upperLimitType;

    /**
     * 分数上限
     */
    private BigDecimal upperLimitScore;

    /**
     * 考核维度数组（含 quotas 指标数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> dimensions;

}
