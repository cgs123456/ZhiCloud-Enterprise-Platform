package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工个人备忘 DO（home 日历，按登录用户隔离）
 *
 * @author zhicloud
 */
@TableName("hr_employee_personal_note")
@KeySequence("hr_employee_personal_note_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeePersonalNoteDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 所属后台用户编号（system_users.id）
     */
    private Long userId;
    /**
     * 备忘内容
     */
    private String content;
    /**
     * 提醒时间
     */
    private LocalDateTime reminderTime;

}
