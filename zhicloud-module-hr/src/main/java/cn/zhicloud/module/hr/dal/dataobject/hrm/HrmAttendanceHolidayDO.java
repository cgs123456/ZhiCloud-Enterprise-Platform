package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDate;

/**
 * HRM 考勤节假日 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_holiday", autoResultMap = false)
@KeySequence("hr_attendance_holiday_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceHolidayDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 日期类型：1上班 2休息
     */
    private Integer type;

}
