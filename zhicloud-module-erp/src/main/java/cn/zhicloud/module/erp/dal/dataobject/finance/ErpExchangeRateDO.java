package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 汇率 DO
 *
 * <p>记录某一时段内源币种到目标币种的汇率。
 * 通过 effectiveDate / expiryDate 控制汇率的有效区间。
 *
 * @author 智云
 */
@TableName("erp_exchange_rate")
@KeySequence("erp_exchange_rate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpExchangeRateDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 源币种编号
     *
     * 关联 {@link ErpCurrencyDO#getId()}
     */
    private Long fromCurrencyId;
    /**
     * 目标币种编号
     *
     * 关联 {@link ErpCurrencyDO#getId()}
     */
    private Long toCurrencyId;
    /**
     * 汇率（fromCurrency -> toCurrency）
     */
    private BigDecimal rate;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expiryDate;
    /**
     * 备注
     */
    private String remark;

}
