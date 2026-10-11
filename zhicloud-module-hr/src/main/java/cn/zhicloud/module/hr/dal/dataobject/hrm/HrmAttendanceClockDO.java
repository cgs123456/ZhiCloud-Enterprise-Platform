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
 * HRM 考勤打卡记录 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_clock", autoResultMap = false)
@KeySequence("hr_attendance_clock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceClockDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 打卡员工编号
     */
    private Long employeeId;

    /**
     * 打卡时间
     */
    private LocalDateTime clockTime;

    /**
     * 打卡类型：1上班 2下班
     */
    private Integer type;

    /**
     * 应打卡日期（当天）
     */
    private LocalDateTime attendanceTime;

    /**
     * 打卡来源：1手机端 2手工录入
     */
    private Integer sourceType;

    /**
     * 打卡状态（hrm_attendance_clock_status 字典）
     */
    private Integer status;

    /**
     * 打卡阶段
     */
    private Integer stage;

    /**
     * 打卡地址
     */
    private String address;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * WiFi 名称
     */
    private String ssid;

    /**
     * WiFi MAC 地址
     */
    private String mac;

    /**
     * 备注
     */
    private String remark;

}
