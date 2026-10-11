package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * HRM 考勤组 DO（班次/地点/WiFi/特殊日期/扣款规则以 json 列整体存）
 *
 * @author zhicloud
 */
@TableName(value = "hr_attendance_group", autoResultMap = true)
@KeySequence("hr_attendance_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmAttendanceGroupDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 考勤组名称
     */
    private String name;

    /**
     * 是否启用 WiFi 打卡
     */
    private Boolean openWifiCard;

    /**
     * 是否启用定位打卡
     */
    private Boolean openPointCard;

    /**
     * 是否法定节假日休息
     */
    private Boolean rest;

    /**
     * 是否默认考勤组
     */
    private Boolean defaultStatus;

    /**
     * 特殊日期配置数组 [{type,date}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> specialDates;

    /**
     * 适用部门编号数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> deptIds;

    /**
     * 适用员工编号数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> employeeIds;

    /**
     * 班次配置数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> shifts;

    /**
     * 打卡地点数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> points;

    /**
     * 打卡 WiFi 数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> wifis;

    /**
     * 扣款规则对象
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> deductRule;

}
