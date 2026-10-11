package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HRM 绩效计划 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_plan", autoResultMap = true)
@KeySequence("hr_perf_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformancePlanDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 计划名称
     */
    private String name;

    /**
     * 考核周期类型
     */
    private Integer cycleType;

    /**
     * 考核周期
     */
    private String cycle;

    /**
     * 季度
     */
    private Integer quarter;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 考核说明
     */
    private String description;

    /**
     * 考评范围列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> scopes;

    /**
     * 考核模板编号
     */
    private Long assessmentTemplateId;

    /**
     * 考核配置快照
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> assessmentConfig;

    /**
     * 结果模板编号
     */
    private Long resultTemplateId;

    /**
     * 结果配置快照
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resultConfig;

    /**
     * 指标制定方式
     */
    private Integer quotaSettingType;

    /**
     * 是否开启目标确认
     */
    private Boolean targetConfirmation;

    /**
     * 目标确认节点
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> targetConfirmationStage;

    /**
     * 评分阶段列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> reviewStages;

    /**
     * 是否开启结果审核
     */
    private Boolean resultAudit;

    /**
     * 结果审核节点列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> resultAuditStages;

    /**
     * 是否开启结果确认
     */
    private Boolean resultConfirmation;

    /**
     * 申诉确认节点列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> appealStages;

    /**
     * 申诉超期天数
     */
    private Integer appealTimeoutDays;

    /**
     * 申诉超期处理动作
     */
    private Integer appealTimeoutAction;

    /**
     * 是否同步薪资
     */
    private Boolean syncToSalary;

    /**
     * 计薪月份
     */
    private String paidForMonth;

    /**
     * 当前阶段
     */
    private Integer stageType;

    /**
     * 计划状态：1草稿 2未开始 3进行中 4已归档 5已终止
     */
    private Integer status;

    /**
     * 终止时间
     */
    private LocalDateTime terminateTime;

}
