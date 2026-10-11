package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工培训经历 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_training_experience")
@KeySequence("hr_employee_training_experience_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeTrainingExperienceDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 员工编号
     */
    private Long employeeId;
    /**
     * 培训课程
     */
    private String course;
    /**
     * 培训机构名称
     */
    private String organizationName;
    /**
     * 培训开始日期
     */
    private LocalDateTime startTime;
    /**
     * 培训结束日期
     */
    private LocalDateTime endTime;
    /**
     * 培训时长
     */
    private String duration;
    /**
     * 培训成绩
     */
    private String result;
    /**
     * 培训证书名称
     */
    private String certificateName;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
