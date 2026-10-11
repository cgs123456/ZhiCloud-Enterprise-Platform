package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * HRM 工资条模板 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_slip_template", autoResultMap = true)
@KeySequence("hr_salary_slip_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalarySlipTemplateDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 是否隐藏空工资项
     */
    private Boolean hideEmpty;

    /**
     * 是否默认模板
     */
    private Boolean defaultStatus;

    /**
     * 模板工资项
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> options;

}
