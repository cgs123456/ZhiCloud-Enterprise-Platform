package cn.zhicloud.module.hr.dal.dataobject.leave;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("hr_leave_balance")
@KeySequence("hr_leave_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveBalanceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long employeeId;
    private Long leaveTypeId;
    private Integer year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
    private String remark;

}