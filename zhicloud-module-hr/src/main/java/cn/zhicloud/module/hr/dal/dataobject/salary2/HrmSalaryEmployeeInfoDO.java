package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HRM 员工薪资档案 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_employee_info", autoResultMap = true)
@KeySequence("hr_salary_employee_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryEmployeeInfoDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 调薪原因
     */
    private Integer changeReason;

    /**
     * 调薪生效时间
     */
    private LocalDateTime effectTime;

    /**
     * 调薪类型
     */
    private Integer changeType;

    /**
     * 试用期工资
     */
    private BigDecimal probationSalary;

    /**
     * 转正工资
     */
    private BigDecimal regularSalary;

    /**
     * 备注
     */
    private String remark;

    /**
     * 转正工资项
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> salaryOptions;

    /**
     * 试用期工资项
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> probationSalaryOptions;

}
