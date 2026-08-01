package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 银行账户 DO（P0-3 资金管理）
 *
 * @author 芋道源码
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
     * 状态（0 启用 1 禁用）
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}