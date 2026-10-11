package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


/**
 * HRM 社保方案 DO
 *
 * @author zhicloud
 */
@TableName("hr_insurance_scheme")
@KeySequence("hr_insurance_scheme_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceSchemeDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 方案名称
     */
    private String name;

    /**
     * 参保地区编号（system_area.id，areaName 现算不入库）
     */
    private Long areaId;

    /**
     * 户籍类型（标准参保类型 code）
     */
    private String householdType;

    /**
     * 方案类型：1比例 2固定金额
     */
    private Integer type;

    /**
     * 个人社保金额（汇总）
     */
    private java.math.BigDecimal personalInsuranceAmount;

    /**
     * 公司社保金额（汇总）
     */
    private java.math.BigDecimal corporateInsuranceAmount;

    /**
     * 个人公积金金额（汇总）
     */
    private java.math.BigDecimal personalProvidentFundAmount;

    /**
     * 公司公积金金额（汇总）
     */
    private java.math.BigDecimal corporateProvidentFundAmount;

}
