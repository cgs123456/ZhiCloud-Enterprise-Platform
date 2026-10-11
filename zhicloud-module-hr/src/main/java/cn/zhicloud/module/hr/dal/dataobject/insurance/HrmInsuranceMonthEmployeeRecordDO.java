package cn.zhicloud.module.hr.dal.dataobject.insurance;

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
 * HRM 员工月度社保记录 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_insurance_month_employee_record", autoResultMap = true)
@KeySequence("hr_ins_month_emp_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceMonthEmployeeRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 月度社保表编号
     */
    private Long monthRecordId;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 社保方案编号
     */
    private Long schemeId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 个人社保金额
     */
    private BigDecimal personalInsuranceAmount;

    /**
     * 个人公积金金额
     */
    private BigDecimal personalProvidentFundAmount;

    /**
     * 公司社保金额
     */
    private BigDecimal corporateInsuranceAmount;

    /**
     * 公司公积金金额
     */
    private BigDecimal corporateProvidentFundAmount;

    /**
     * 参保状态：0停止 1正常
     */
    private Integer status;

    /**
     * 社保项目
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> socialSecurityProjects;

    /**
     * 公积金项目
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> providentFundProjects;

}
