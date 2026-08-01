package cn.iocoder.yudao.module.qms.dal.dataobject.training;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.TrainingPlanStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 培训计划 DO
 *
 * <p>年度培训计划与课程安排。
 *
 * @author yudao
 */
@TableName("qms_training_plan")
@KeySequence("qms_training_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlanDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 计划编号
     */
    private String planNo;
    /**
     * 计划名称
     */
    private String planName;
    /**
     * 年度
     */
    private Integer year;
    /**
     * 课程名称
     */
    private String courseName;
    /**
     * 讲师
     */
    private String trainer;
    /**
     * 计划日期
     */
    private LocalDate planDate;
    /**
     * 状态
     *
     * 枚举 {@link TrainingPlanStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}