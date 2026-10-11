package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;

/**
 * HRM 月度考勤汇总 DO（(employee,year,month) 唯一）
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_month_record", autoResultMap = false)
@KeySequence("hr_attendance_month_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceMonthRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 应出勤天数
     */
    private BigDecimal attendDays;

    /**
     * 实际出勤天数
     */
    private BigDecimal actualDays;

    /**
     * 迟到分钟数
     */
    private Integer lateMinute;

    /**
     * 迟到次数
     */
    private Integer lateCount;

    /**
     * 早退分钟数
     */
    private Integer earlyMinute;

    /**
     * 早退次数
     */
    private Integer earlyCount;

    /**
     * 缺卡次数
     */
    private Integer misscardCount;

    /**
     * 旷工天数
     */
    private BigDecimal absenteeismDays;

    /**
     * 旷工分钟数
     */
    private Integer absenteeismMinutes;

    /**
     * 请假天数
     */
    private BigDecimal leaveDays;

    /**
     * 请假分钟数
     */
    private Integer leaveMinutes;

    /**
     * 迟到扣款
     */
    private BigDecimal lateDeductAmount;

    /**
     * 早退扣款
     */
    private BigDecimal earlyDeductAmount;

    /**
     * 缺卡扣款
     */
    private BigDecimal misscardDeductAmount;

    /**
     * 旷工扣款
     */
    private BigDecimal absenteeismDeductAmount;

    /**
     * 考勤扣款合计
     */
    private BigDecimal attendanceDeductAmount;

    /**
     * 是否全勤
     */
    private Boolean fullAttendance;

}
