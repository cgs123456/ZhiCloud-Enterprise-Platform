package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * HRM 员工月度工资 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_month_employee_record", autoResultMap = true)
@KeySequence("hr_salary_month_emp_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryMonthEmployeeRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 月度工资表编号
     */
    private Long monthRecordId;

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
     * 实际出勤天数
     */
    private BigDecimal actualWorkDay;

    /**
     * 应出勤天数
     */
    private BigDecimal needWorkDay;

    /**
     * 应发工资
     */
    private BigDecimal expectedPaySalary;

    /**
     * 应税工资
     */
    private BigDecimal taxableSalary;

    /**
     * 个人所得税
     */
    private BigDecimal personalTax;

    /**
     * 实发工资
     */
    private BigDecimal realPaySalary;

    /**
     * 绩效系数
     */
    private BigDecimal performanceCoefficient;

    /**
     * 工资项值
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> optionValues;

}
