package cn.iocoder.yudao.module.hr.dal.dataobject.leave;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("hr_leave_type")
@KeySequence("hr_leave_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveTypeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String code;
    private Integer isPaid;
    private Integer deductSalary;
    private String remark;

}