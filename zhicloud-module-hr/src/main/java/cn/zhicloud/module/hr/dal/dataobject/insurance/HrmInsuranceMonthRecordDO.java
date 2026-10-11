package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;

/**
 * HRM 月度社保表 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_insurance_month_record", autoResultMap = false)
@KeySequence("hr_ins_month_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceMonthRecordDO extends TenantBaseDO {

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
     * 参保人数
     */
    private Integer insuredEmployeeCount;

    /**
     * 停止参保人数
     */
    private Integer stoppedEmployeeCount;

    /**
     * 状态
     */
    private Integer status;

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

}
