package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 考勤组员工关联 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_group_employee", autoResultMap = false)
@KeySequence("hr_attendance_group_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceGroupEmployeeDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 考勤组编号
     */
    private Long groupId;

    /**
     * 员工编号
     */
    private Long employeeId;

}
