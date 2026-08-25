package cn.zhicloud.module.hr.dal.dataobject.attendance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.hr.enums.attendance.HrAttendanceStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR 考勤记录 DO
 *
 * @author zhicloud
 */
@TableName("hr_attendance")
@KeySequence("hr_attendance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendanceDO extends TenantBaseDO {

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
     * 考勤日期
     */
    private LocalDate attendanceDate;
    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;
    /**
     * 签退时间
     */
    private LocalDateTime checkOutTime;
    /**
     * 状态
     *
     * 枚举 {@link HrAttendanceStatusEnum}
     */
    private Integer status;
    /**
     * 加班时长（小时）
     */
    private BigDecimal overtimeHours;
    /**
     * 备注
     */
    private String remark;

}