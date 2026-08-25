package cn.zhicloud.module.hr.dal.dataobject.socialinsurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("hr_social_insurance_base")
@KeySequence("hr_social_insurance_base_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrSocialInsuranceBaseDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long employeeId;
    private Integer year;
    private BigDecimal pensionBase;
    private BigDecimal medicalBase;
    private BigDecimal unemploymentBase;
    private BigDecimal workInjuryBase;
    private BigDecimal maternityBase;
    private BigDecimal housingFundBase;
    private BigDecimal personalPensionRate;
    private BigDecimal companyPensionRate;
    private BigDecimal personalMedicalRate;
    private BigDecimal companyMedicalRate;
    private BigDecimal personalUnemploymentRate;
    private BigDecimal companyUnemploymentRate;
    private BigDecimal companyWorkInjuryRate;
    private BigDecimal companyMaternityRate;
    private BigDecimal personalHousingFundRate;
    private BigDecimal companyHousingFundRate;
    private Integer status;
    private String remark;

}