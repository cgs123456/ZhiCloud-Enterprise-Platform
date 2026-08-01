package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 获利能力分析 DO
 *
 * <p>按利润中心 + 期间维度记录收入、成本与利润，并维护利润率。
 *
 * @author 芋道源码
 */
@TableName("erp_profitability_analysis")
@KeySequence("erp_profitability_analysis_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpProfitabilityAnalysisDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 利润中心编号
     *
     * 关联 {@link ErpProfitCenterDO#getId()}
     */
    private Long profitCenterId;
    /**
     * 会计期间编号
     *
     * 关联 {@link ErpPeriodDO#getId()}
     */
    private Long periodId;
    /**
     * 收入
     */
    private BigDecimal revenue;
    /**
     * 成本
     */
    private BigDecimal cost;
    /**
     * 利润
     */
    private BigDecimal profit;
    /**
     * 利润率（profit / revenue）
     */
    private BigDecimal profitMargin;
    /**
     * 备注
     */
    private String remark;

}
