package cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpTaxRateTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 税率 DO
 *
 * @author 芋道源码
 */
@TableName("erp_tax_rate")
@KeySequence("erp_tax_rate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpTaxRateDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    private Integer rateType;
    private BigDecimal rate;
    private Integer isDefault;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String remark;
    private Integer status;

}
