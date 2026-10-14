package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 银行账户 DO（P0-3 资金管理）
 *
 * @author 智云
 */
@TableName("erp_bank_account")
@KeySequence("erp_bank_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBankAccountDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 账号
     */
    private String accountNo;
    /**
     * 账户名称
     */
    private String accountName;
    /**
     * 开户行
     */
    private String bankName;
    /**
     * 开户支行
     */
    private String bankBranch;
    /**
     * 账户余额
     */
    private BigDecimal balance;
    /**
     * 币种编号
     */
    private Long currencyId;
    /**
     * 账簿编号（可空；配了账簿的账户，其流水才参与账套视角现金流量表，见 V195）
     *
     * <p>V195（台账 §12.215）：erp_cash_flow 只到 bank_account_id 没有账簿关联，
     * 给账户挂账簿后报表侧按「账簿 → 账户集合 → 流水」过滤，存量流水随账户
     * 配置立即可见，无需回填流水表。
     */
    private Long accountBookId;
    /**
     * 状态（0 启用 1 禁用）
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}