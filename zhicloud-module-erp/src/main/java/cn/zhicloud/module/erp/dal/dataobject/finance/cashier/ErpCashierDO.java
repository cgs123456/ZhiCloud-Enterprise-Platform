package cn.zhicloud.module.erp.dal.dataobject.finance.cashier;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 出纳单 DO
 *
 * @author 智云
 */
@TableName("erp_cashier")
@KeySequence("erp_cashier_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCashierDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 出纳单号
     */
    private String no;
    /**
     * 出纳类型
     *
     * 10 收款 / 20 付款 / 30 内部转账
     */
    private Integer cashierType;
    /**
     * 银行账户编号
     *
     * 关联 {@link ErpBankAccountDO#getId()}
     */
    private Long bankAccountId;
    /**
     * 对方名称
     */
    private String counterpartyName;
    /**
     * 对方账号
     */
    private String counterpartyAccount;
    /**
     * 对方开户行
     */
    private String counterpartyBank;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 支付方式
     *
     * 10 现金 / 20 转账 / 30 支票 / 40 网银
     */
    private Integer paymentMethod;
    /**
     * 支付日期
     */
    private LocalDate paymentDate;
    /**
     * 状态
     *
     * 10 待处理 / 20 已提交银行 / 30 已到账 / 40 已退回
     */
    private Integer status;
    /**
     * 银行流水号（网银直联返回）
     */
    private String bankSerialNo;
    /**
     * 关联业务单号
     */
    private String businessOrderNo;
    /**
     * 备注
     */
    private String remark;

}
