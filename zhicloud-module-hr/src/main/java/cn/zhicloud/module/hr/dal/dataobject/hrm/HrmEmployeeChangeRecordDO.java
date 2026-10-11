package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工异动记录 DO（转正/调岗/晋升/降级/转全职/再入职）
 *
 * @author zhicloud
 */
@TableName("hr_employee_change_record")
@KeySequence("hr_employee_change_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeChangeRecordDO extends TenantBaseDO {

    /**
     * 异动类型：4转正 5调岗 6晋升 7降级 8转为全职 9再入职
     */
    public static final int TYPE_REGULAR = 4;
    public static final int TYPE_TRANSFER = 5;
    public static final int TYPE_PROMOTE = 6;
    public static final int TYPE_DEMOTE = 7;
    public static final int TYPE_CONVERT_FULL_TIME = 8;
    public static final int TYPE_REHIRE = 9;

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
     * 异动类型
     */
    private Integer type;
    /**
     * 异动原因：1组织架构调整 2个人申请 3工作安排 4违规违纪 5绩效不达标 6身体原因 7不适应岗位
     */
    private Integer reason;
    /**
     * 原部门编号
     */
    private Long oldDeptId;
    /**
     * 原部门名称（快照）
     */
    private String oldDeptName;
    /**
     * 新部门编号
     */
    private Long newDeptId;
    /**
     * 新部门名称（快照）
     */
    private String newDeptName;
    /**
     * 原职位
     */
    private String oldPostName;
    /**
     * 新职位
     */
    private String newPostName;
    /**
     * 原岗位职级
     */
    private String oldPostLevel;
    /**
     * 新岗位职级
     */
    private String newPostLevel;
    /**
     * 原工作地点
     */
    private String oldWorkAddress;
    /**
     * 新工作地点
     */
    private String newWorkAddress;
    /**
     * 原直属上级员工编号
     */
    private Long oldLeaderEmployeeId;
    /**
     * 原直属上级姓名（快照）
     */
    private String oldLeaderEmployeeName;
    /**
     * 新直属上级员工编号
     */
    private Long newLeaderEmployeeId;
    /**
     * 新直属上级姓名（快照）
     */
    private String newLeaderEmployeeName;
    /**
     * 试用期（月，转全职时记录）
     */
    private Integer probation;
    /**
     * 生效日期
     */
    private LocalDateTime effectTime;
    /**
     * 备注
     */
    private String remark;

}
