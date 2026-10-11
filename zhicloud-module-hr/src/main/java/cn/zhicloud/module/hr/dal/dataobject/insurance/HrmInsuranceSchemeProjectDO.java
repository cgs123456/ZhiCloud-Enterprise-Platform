package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


/**
 * HRM 社保方案项目 DO
 *
 * @author zhicloud
 */
@TableName("hr_insurance_scheme_project")
@KeySequence("hr_insurance_scheme_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceSchemeProjectDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 社保方案编号
     */
    private Long schemeId;

    /**
     * 项目类型（1-11，hrm_insurance_project_type 字典）
     */
    private Integer type;

    /**
     * 项目名称（自定义项目可改）
     */
    private String name;

    /**
     * 缴纳基数（元）
     */
    private java.math.BigDecimal baseAmount;

    /**
     * 公司缴纳比例（小数）
     */
    private java.math.BigDecimal corporateRate;

    /**
     * 个人缴纳比例（小数）
     */
    private java.math.BigDecimal personalRate;

    /**
     * 公司缴纳金额（元）
     */
    private java.math.BigDecimal corporateAmount;

    /**
     * 个人缴纳金额（元）
     */
    private java.math.BigDecimal personalAmount;

}
