package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HRM 考勤请假 DO（员工自助发起走 BPM）
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_leave", autoResultMap = false)
@KeySequence("hr_attendance_leave_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceLeaveDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 请假类型（hrm_attendance_leave_type 字典 value）
     */
    private String type;

    /**
     * 请假开始时间
     */
    private LocalDateTime startTime;

    /**
     * 请假结束时间
     */
    private LocalDateTime endTime;

    /**
     * 请假天数
     */
    private BigDecimal day;

    /**
     * 请假事由
     */
    private String reason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 审批状态
     */
    private Integer approvalStatus;

    /**
     * 流程实例编号
     */
    private String processInstanceId;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 审批意见
     */
    private String approvalReason;

}
