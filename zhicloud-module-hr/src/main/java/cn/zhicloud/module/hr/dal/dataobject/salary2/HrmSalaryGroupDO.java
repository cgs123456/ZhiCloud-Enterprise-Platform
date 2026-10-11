package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * HRM 薪资组 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_group", autoResultMap = true)
@KeySequence("hr_salary_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryGroupDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 薪资组名称
     */
    private String name;

    /**
     * 月计薪标准
     */
    private Integer salaryStandard;

    /**
     * 转正、调薪月规则
     */
    private String changeRule;

    /**
     * 计税规则编号
     */
    private Long taxRuleId;

    /**
     * 适用部门编号列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> deptIds;

    /**
     * 适用员工编号列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> employeeIds;

}
