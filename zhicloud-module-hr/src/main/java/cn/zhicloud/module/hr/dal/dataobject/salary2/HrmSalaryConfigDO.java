package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 薪资配置 DO（单条）
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_config", autoResultMap = false)
@KeySequence("hr_salary_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryConfigDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 计薪周期开始日
     */
    private Integer cycleStartDay;

    /**
     * 计薪周期结束日
     */
    private Integer cycleEndDay;

    /**
     * 社保对应月份类型
     */
    private Integer socialSecurityMonthType;

    /**
     * 工资开始年份
     */
    private Integer startYear;

    /**
     * 工资开始月份
     */
    private Integer startMonth;

}
