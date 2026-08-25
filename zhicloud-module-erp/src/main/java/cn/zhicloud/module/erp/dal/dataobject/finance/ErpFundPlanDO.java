package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 资金计划 DO（P0-3 资金管理）
 *
 * @author 智云
 */
@TableName("erp_fund_plan")
@KeySequence("erp_fund_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFundPlanDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 计划期间（如 2026-07）
     */
    private String planPeriod;
    /**
     * 计划类型
     *
     * 枚举 {@link cn.zhicloud.module.erp.enums.finance.ErpFundPlanTypeEnum}
     */
    private Integer planType;
    /**
     * 计划金额
     */
    private BigDecimal amount;
    /**
     * 银行账户编号
     */
    private Long bankAccountId;
    /**
     * 备注
     */
    private String remark;

}