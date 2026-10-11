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
 * HRM 工资条发放记录 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_slip_send_record", autoResultMap = true)
@KeySequence("hr_salary_slip_send_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalarySlipSendRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 月度工资表编号
     */
    private Long monthRecordId;

    /**
     * 工资条数量
     */
    private Integer employeeCount;

    /**
     * 发放数量
     */
    private Integer sendEmployeeCount;

    /**
     * 已查看数量
     */
    private Integer readCount;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 本次发放的工资条模板项
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> options;

}
