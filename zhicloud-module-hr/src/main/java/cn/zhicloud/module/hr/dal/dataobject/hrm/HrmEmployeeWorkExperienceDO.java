package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工工作经历 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_work_experience")
@KeySequence("hr_employee_work_experience_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeWorkExperienceDO extends TenantBaseDO {

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
     * 工作单位
     */
    private String workUnit;
    /**
     * 职务
     */
    private String postName;
    /**
     * 工作开始日期
     */
    private LocalDateTime startTime;
    /**
     * 工作结束日期
     */
    private LocalDateTime endTime;
    /**
     * 离职原因
     */
    private String reason;
    /**
     * 证明人
     */
    private String witnessName;
    /**
     * 证明人手机号
     */
    private String witnessPhone;
    /**
     * 工作备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
