package cn.iocoder.yudao.module.hr.dal.dataobject.performance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.hr.enums.performance.HrPerformanceGradeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HR 绩效记录 DO
 *
 * @author yudao
 */
@TableName("hr_performance")
@KeySequence("hr_performance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrPerformanceDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 员工 ID
     */
    private Long employeeId;
    /**
     * 考核周期（yyyyMM 月度 或 yyyyQn 季度，如 2024Q1）
     */
    private String period;
    /**
     * 考核得分
     */
    private BigDecimal score;
    /**
     * 考核等级
     *
     * 枚举 {@link HrPerformanceGradeEnum}
     */
    private Integer grade;
    /**
     * 考核人 ID
     */
    private Long evaluatorId;
    /**
     * 考核日期
     */
    private LocalDate evaluationDate;
    /**
     * 考核意见
     */
    private String comment;
    /**
     * 备注
     */
    private String remark;

}