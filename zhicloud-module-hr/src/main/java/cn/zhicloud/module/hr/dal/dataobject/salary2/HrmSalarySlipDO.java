package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 员工工资条 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_slip", autoResultMap = false)
@KeySequence("hr_salary_slip_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalarySlipDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 工资条发放记录编号
     */
    private Long sendRecordId;

    /**
     * 员工月度工资记录编号
     */
    private Long monthEmployeeRecordId;

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
     * 查看状态
     */
    private Integer readStatus;

}
