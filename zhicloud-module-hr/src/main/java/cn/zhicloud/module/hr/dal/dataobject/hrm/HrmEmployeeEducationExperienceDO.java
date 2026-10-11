package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工教育经历 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_education_experience")
@KeySequence("hr_employee_education_experience_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeEducationExperienceDO extends TenantBaseDO {

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
     * 学历（1-12，见 hrm_employee_education 字典）
     */
    private Integer education;
    /**
     * 毕业院校
     */
    private String graduateSchool;
    /**
     * 专业
     */
    private String major;
    /**
     * 入学日期
     */
    private LocalDateTime admissionTime;
    /**
     * 毕业日期
     */
    private LocalDateTime graduationTime;
    /**
     * 教学方式
     */
    private Integer teachingMethods;
    /**
     * 是否第一学历
     */
    private Boolean firstDegree;
    /**
     * 排序
     */
    private Integer sort;

}
