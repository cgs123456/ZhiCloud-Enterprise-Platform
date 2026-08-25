package cn.zhicloud.module.hr.dal.dataobject.leave;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("hr_leave_request")
@KeySequence("hr_leave_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveRequestDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long employeeId;
    private Long leaveTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;
    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;

}