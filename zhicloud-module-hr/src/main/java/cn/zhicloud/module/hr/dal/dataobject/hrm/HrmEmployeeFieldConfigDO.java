package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * HRM 员工字段显示配置 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_field_config")
@KeySequence("hr_employee_field_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeFieldConfigDO extends TenantBaseDO {

    /**
     * 配置种类：1档案字段 2新建员工字段
     */
    public static final int KIND_ARCHIVE = 1;
    public static final int KIND_CREATE = 2;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 配置种类
     */
    private Integer kind;
    /**
     * 入职状态（kind=2 时区分在职 1/待入职 2 表单；kind=1 为 NULL）
     */
    private Integer entryStatus;
    /**
     * 字段名称（与前端 HrmEmployeeVO 属性名一致）
     */
    private String name;
    /**
     * 字段标题
     */
    private String title;
    /**
     * 字段分组名称
     */
    private String groupName;
    /**
     * 是否显示
     */
    private Boolean visible;
    /**
     * 是否允许员工编辑
     */
    private Boolean editable;
    /**
     * 是否锁定显示
     */
    private Boolean visibleLocked;
    /**
     * 是否锁定编辑
     */
    private Boolean editableLocked;
    /**
     * 排序
     */
    private Integer sort;

}
