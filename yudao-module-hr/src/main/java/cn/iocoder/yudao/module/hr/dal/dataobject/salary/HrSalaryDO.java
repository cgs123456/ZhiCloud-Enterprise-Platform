package cn.iocoder.yudao.module.hr.dal.dataobject.salary;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.hr.enums.salary.HrSalaryStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * HR 薪资记录 DO
 *
 * @author yudao
 */
@TableName("hr_salary")
@KeySequence("hr_salary_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrSalaryDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 员工 ID
     */
    private Long employeeId;
    /**
     * 薪资月份（yyyyMM）
     */
    private String salaryMonth;
    /**
     * 基本工资
     */
    private BigDecimal baseSalary;
    /**
     * 加班费
     */
    private BigDecimal overtimePay;
    /**
     * 奖金
     */
    private BigDecimal bonus;
    /**
     * 扣款
     */
    private BigDecimal deduction;
    /**
     * 社保
     */
    private BigDecimal socialInsurance;
    /**
     * 公积金
     */
    private BigDecimal housingFund;
    /**
     * 个税
     */
    private BigDecimal tax;
    /**
     * 实发工资
     */
    private BigDecimal netSalary;
    /**
     * 状态
     *
     * 枚举 {@link HrSalaryStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}