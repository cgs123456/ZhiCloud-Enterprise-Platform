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
 * HRM 月度工资表 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_month_record", autoResultMap = true)
@KeySequence("hr_salary_month_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryMonthRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 计薪人数
     */
    private Integer employeeCount;

    /**
     * 计薪开始时间
     */
    private LocalDateTime startTime;

    /**
     * 计薪结束时间
     */
    private LocalDateTime endTime;

    /**
     * 应发工资
     */
    private BigDecimal expectedPaySalary;

    /**
     * 个人社保
     */
    private BigDecimal personalInsuranceAmount;

    /**
     * 个人公积金
     */
    private BigDecimal personalProvidentFundAmount;

    /**
     * 个人所得税
     */
    private BigDecimal personalTax;

    /**
     * 实发工资
     */
    private BigDecimal realPaySalary;

    /**
     * 公司社保
     */
    private BigDecimal corporateInsuranceAmount;

    /**
     * 公司公积金
     */
    private BigDecimal corporateProvidentFundAmount;

    /**
     * 工资表状态
     */
    private Integer status;

    /**
     * 工资项表头
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> optionHeaders;

}
