package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;

/**
 * HRM 计税规则 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_tax_rule", autoResultMap = false)
@KeySequence("hr_salary_tax_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryTaxRuleDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 计税规则名称
     */
    private String name;

    /**
     * 计税类型
     */
    private Integer type;

    /**
     * 是否计税
     */
    private Boolean taxEnabled;

    /**
     * 起征阈值
     */
    private BigDecimal threshold;

    /**
     * 小数位数
     */
    private Integer decimalScale;

    /**
     * 计税周期类型
     */
    private Integer cycleType;

}
