package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 现金流记录 DO（P0-3 资金管理）
 *
 * @author 智云
 */
@TableName("erp_cash_flow")
@KeySequence("erp_cash_flow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCashFlowDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 业务类型
     *
     * 枚举 {@link cn.zhicloud.module.erp.enums.finance.ErpCashFlowBizTypeEnum}
     */
    private Integer bizType;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 银行账户编号
     */
    private Long bankAccountId;
    /**
     * 业务单据编号
     */
    private Long bizOrderId;
    /**
     * 业务单据类型
     */
    private String bizOrderType;
    /**
     * 发生日期
     */
    private LocalDate occurDate;
    /**
     * 备注
     */
    private String remark;

}