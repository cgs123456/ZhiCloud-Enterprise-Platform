package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 期初余额 DO（账套口径，C6③ / ADR 265）
 *
 * <p>粒度 (tenant_id, account_set_id, account_id)；account_set_id 弱引用 FMS
 * fms_account_set（无外键，V191 账簿先例）。与 erp_gl_account 的
 * opening_* / current_* / closing_*（租户口径、凭证审核实时维护）两轨并存、暂不互写。
 *
 * <p>「年初余额」为派生只读、不入库：借向科目 = opening - yearDebit + yearCredit；
 * 其余方向（贷向 / 双向）= opening + yearDebit - yearCredit。
 *
 * @author 智云
 */
@TableName("erp_gl_initial_balance")
@KeySequence("erp_gl_initial_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpGlInitialBalanceDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * FMS 账套编号（弱引用 fms_account_set.id，无外键）
     */
    private Long accountSetId;
    /**
     * 会计科目编号（erp_gl_account.id，末级科目）
     */
    private Long accountId;
    /**
     * 期初余额（账套启用期间的期初）
     */
    private BigDecimal openingAmount;
    /**
     * 本年累计借方发生额（启用前发生额，仅年中启用非零）
     */
    private BigDecimal yearDebitAmount;
    /**
     * 本年累计贷方发生额（启用前发生额，仅年中启用非零）
     */
    private BigDecimal yearCreditAmount;
    /**
     * 实际损益发生额（损益类科目）
     */
    private BigDecimal profitLossAmount;

}
