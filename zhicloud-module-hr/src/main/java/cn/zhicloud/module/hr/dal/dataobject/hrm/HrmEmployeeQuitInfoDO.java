package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工离职信息 DO（一员工一条）
 *
 * @author zhicloud
 */
@TableName("hr_employee_quit_info")
@KeySequence("hr_employee_quit_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeQuitInfoDO extends TenantBaseDO {

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
     * 计划离职时间
     */
    private LocalDateTime planQuitTime;
    /**
     * 申请离职时间
     */
    private LocalDateTime applyQuitTime;
    /**
     * 薪资结算时间
     */
    private LocalDateTime salarySettlementTime;
    /**
     * 离职类型：1主动 2被动 3退休
     */
    private Integer type;
    /**
     * 离职原因（1-16，见前端 HrmEmployeeQuitReason）
     */
    private Integer reason;
    /**
     * 离职前员工状态
     */
    private Integer oldEmployeeStatus;
    /**
     * 备注
     */
    private String remark;

}
